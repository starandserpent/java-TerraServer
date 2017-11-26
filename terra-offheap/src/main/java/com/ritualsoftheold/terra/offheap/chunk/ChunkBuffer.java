package com.ritualsoftheold.terra.offheap.chunk;

import java.util.concurrent.atomic.AtomicInteger;

import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer.Allocator;
import com.ritualsoftheold.terra.offheap.chunk.compress.ChunkFormat;
import com.ritualsoftheold.terra.offheap.memory.MemoryUseListener;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Contains chunks in memory.
 *
 */
public class ChunkBuffer {
    
    private static final Memory mem = OS.memory();
    
    /**
     * Chunk memory addresses. Pointer to data.
     */
    private long addrs;
    
    /**
     * Chunk data lengths. Pointer to data.
     */
    private long lengths;
    
    /**
     * Chunk types. Note that not all chunk types have associated address!
     * Pointer to data.
     */
    private long types;
    
    /**
     * Change queues for all chunks.
     * Pointer to data.
     */
    private long queues;
    
    private int queueSize;
    
    /**
     * Current count of chunks in this buffer. This also serves
     * as first free index to this buffer.
     */
    private AtomicInteger chunkCount;
    
    /**
     * Maximum count of chunks in this buffer.
     */
    private int maxCount;
    
    /**
     * If of this chunk buffer.
     */
    private int bufferId;
    
    /**
     * Returns when this buffer was last used.
     */
    private volatile long lastNeeded;
    
    /**
     * Memory usage listener.
     */
    private MemoryUseListener memListener;
    
    private class ChangeQueue {
        
        /*
         * Internally chunk changes are put into buffer-global queue,
         * which is stored in offheap memory.
         * 
         * Each query is 8 bytes (a long). Contents are:
         * * 1 byte: type of query
         * 
         * Following type comes other data. For '0' queries, it is:
         * * 2 bytes: affected chunk (in this buffer)
         * * 3 bytes: block index in the chunk
         * * 2 bytes: block id to set
         * 
         * Write and read everything as single long, otherwise endianness issues may arise!
         * 
         * Queries are to be manually flushed periodically.
         */
        
        /**
         * Queue data address.
         */
        private long addr;
        
        /**
         * When flushing, data is copied here.
         */
        private long flushAddr;
        
        /**
         * How many elements fit into the queue at same time.
         */
        private int size;
        
        /**
         * Current free position in the queue.
         */
        private AtomicInteger pos;
        
        private ChangeQueue(long addr1, long addr2, int size) {
            this.addr = addr1;
            this.flushAddr = addr2;
            this.size = size;
            this.pos = new AtomicInteger(0);
        }
        
        /**
         * Prepares to flush entries.
         * @return How many are to be flushed safely.
         */
        private int prepareFlush() {
            // Copy data to flush cache
            int flushCount = pos.get();
            mem.copyMemory(addr, flushAddr, flushCount * 8);
            
            return flushCount;
        }
        
        private void cleanup(int oldCount) {
            // Zero beginning block (data is flushed already)
            mem.setMemory(addr, oldCount * 8, (byte) 0);
            
            // Zero the position
            int newCount = pos.getAndSet(0);
            
            // Re-add any remaining queries to queue
            int diff = newCount - oldCount;
            for (int i = newCount; i < newCount + diff; i++) {
                add(mem.readVolatileLong(addr + i * 8));
            }
        }

        private void flush() {
            int beginChunkCount = chunkCount.get();
            int flushCount = prepareFlush();
            int processed = 0; // How many of flushes have we queued forward?

            // Actual flushing operation
            while (processed < flushCount) {
                int[] consumed = new int[chunkCount.get()]; // How much have we consumed of per-chunk queues?
                
                for (int i = 0; i < flushCount; i++) {
                    long query = mem.readVolatileLong(flushAddr + i * 8);
                    long type = query >>> 56;
                    
                    if (type == 0) {
                        // Read data using bitwise operations
                        int chunk = (int) (query >>> 40 & 0xffff);
    //                    long block = query >>> 16 & 0xffffff;
    //                    long newId = query & 0xffff;
                        
                        int offset = consumed[chunk];
                        if (offset == queueSize) { // Out of space...
                            break; // Need to update that chunk NOW
                            // TODO optimize
                        }
                        
                        long queueSlot = queues + queueSize * chunk + offset;
                        mem.writeLong(queueSlot, query);
                        
                        // Increment counters
                        consumed[chunk] += 8;
                        processed++;
                    }
                }
                
                // TODO multithreaded (going to have so much "fun" with that...)
                for (int i = 0; i < beginChunkCount; i++) {
                    int queriesSize = consumed[i]; // Data consumed by queries
                    if (queriesSize == 0) {
                        continue; // No changes here
                    }
                    
                    long queueAddr = queues + i * queueSize;
                    
                    // Get suitable chunk format, which we'll eventually use to write the data
                    ChunkFormat format = ChunkFormat.forType(getChunkType(i));
                    
                    // Ask format to process queries (and hope it handles that correctly)
                    ChunkFormat.ProcessResult result = format.processQueries(getChunkAddr(i), getChunkLength(i),
                            allocator, queueAddr, queriesSize);
                    
                    // TODO maybe wrap these in if and have -1 as default (do not change anything) value
                    setChunkLength(i, result.length);
                    setChunkType(i, (byte) result.type);
                    setChunkAddr(i, result.address);
                }
            }

            cleanup(flushCount);
        }
        
        private void add(long query) {
            int index = pos.getAndIncrement();
            if (index > size) {
                pos.decrementAndGet(); // Undo change
                throw new IllegalStateException("change queue full (TODO don't crash but wait instead)");
            }
            mem.writeVolatileLong(addr + index * 8, query);
        }
    }
    
    /**
     * Allocates and deallocates memory for chunks on demand.
     *
     */
    public class Allocator {
        
        // TODO optimize to recycle memory
        
        /**
         * Allocates memory for a chunk with given length.
         * @param length Length of data.
         * @return Memory address where to put it.
         */
        public long alloc(int length) {
            memListener.onAllocate(length);
            return mem.allocate(length);
        }
        
        /**
         * Frees memory from chunk data. This is rather low level utily;
         * be careful to NOT free data which may be used
         * @param addr
         * @param length
         */
        public void free(long addr, int length) {
            mem.freeMemory(addr, length);
            memListener.onFree(length);
            
        }
        
        /**
         * Swaps memory address from old to new chunk. Old memory will be recycled.
         * @param chunk Chunk id.
         * @param oldAddr Old chunk address.
         * @param newAddr New chunk address.
         * @param length Length of new chunk.
         * @param used How much of new space is used.
         */
        public void swap(int chunk, long oldAddr, long newAddr, int length, int used) {
            int oldLength = mem.readVolatileInt(lengths + chunk * 4); // Get old length
            
            setChunkAddr(chunk, newAddr);
            // TODO figure out what if another thread accesses data at this moment
            setChunkLength(chunk, length);
            
            // Deallocate (for now) old chunk
            mem.freeMemory(oldAddr, oldLength);
            memListener.onFree(oldLength);
        }
        
        /**
         * Creates a dummy allocator, which will try to use given address
         * if length of first allocation matches length given here.
         * @param addr Address where there is free.
         * @param length Length of free space.
         */
        public Allocator createDummy(long addr, int length) {
            return new DummyAllocator(addr, length);
        }
        
        /**
         * Sometimes chunk formats or related classes just want to pass memory
         * around; no actual allocations are necessary (or wanted). In these cases,
         * requesting a dummy allocator is best way to operate.
         *
         */
        private class DummyAllocator extends Allocator {
            
            /**
             * Address for memory this will try to provide.
             */
            private long dummyAddr;
            
            private int dummyLength;
            
            private DummyAllocator(long addr, int length) {
                this.dummyAddr = addr;
                this.dummyLength = length;
            }
            
            public long alloc(int length) {
                if (length == dummyLength) {
                    dummyLength = -1; // Disallow further dummy allocations
                    return dummyAddr;
                } else {
                    // Get rid of dummy memory
                    // TODO recycle
                    mem.freeMemory(dummyAddr, dummyLength);
                    memListener.onFree(dummyLength);
                    return super.alloc(length); // Do real allocation
                }
            }
        }
    }
    
    private Allocator allocator;
    
    private ChangeQueue changeQueue;
    
    private int staticDataLength;
    
    public ChunkBuffer(int id, int maxChunks, int globalQueueSize, int chunkQueueSize, MemoryUseListener memListener) {
        int globalQueueLen = globalQueueSize * 8;
        int chunkQueueLen = chunkQueueSize * 8 * maxChunks;
        
        bufferId = id; // Set buffer id
        
        // Initialize memory blocks for metadata
        int allocLen = maxChunks * 13 + 2 * globalQueueLen + chunkQueueLen;
        staticDataLength = allocLen;
        long baseAddr = mem.allocate(allocLen);
        addrs = baseAddr; // 8 bytes per chunk
        lengths = baseAddr + 8 * maxChunks; // 4 bytes per chunk
        types = baseAddr + 12 * maxChunks; // 1 byte per chunk
        
        long globalData = baseAddr + 13 * maxChunks;
        
        // Zero/generally set memory that needs it
        mem.setMemory(baseAddr, maxChunks * 16, (byte) 0); // Zero some chunk specific data
        mem.setMemory(types, maxChunks, ChunkType.EMPTY); // Types need to be EMPTY, even if it is not 0
        mem.setMemory(globalData, 2 * globalQueueLen + chunkQueueLen, (byte) 0); // Zero global data
        
        // Initialize counts (max: parameter, current: 0)
        maxCount = maxChunks;
        chunkCount = new AtomicInteger(0);
        
        // Initialize global change queue
        changeQueue = new ChangeQueue(globalData, globalData + globalQueueLen, globalQueueSize);
        
        // Initialize chunk-local queues
        queueSize = chunkQueueLen;
        queues = globalData + globalQueueLen * 2;
        
        // Save ref to memory use listener and notify it
        this.memListener = memListener;
        memListener.onAllocate(allocLen);
        
        // Initialize chunk memory allocator
        this.allocator = new Allocator();
    }
    
    /**
     * Creates a new chunk. It will not have memory address and typo of it is set to
     * empty. If new chunk cannot be created, -1 will be returned.
     * @return Index for chunk in THIS BUFFER.
     */
    public int newChunk() {
        int index = chunkCount.getAndIncrement();
        if (index > maxCount - 1) { // No space...
            chunkCount.decrementAndGet(); // Whoops
            return -1;
        }
        
        return index;
    }
    
    /**
     * Gets free capacity of this chunk buffer. Note that since everything can
     * be asynchronous, you must also check that {@link #newChunk()} doesn't
     * return -1.
     * @return Free capacity.
     */
    public int getFreeCapacity() {
        return Math.max(0, maxCount - chunkCount.get());
    }
    
    public int getChunkCount() {
        return Math.min(chunkCount.get(), maxCount);
    }
    
    public long getChunkAddr(int index) {
        return mem.readVolatileLong(addrs + index * 8);
    }
    
    public void setChunkAddr(int index, long addr) {
        mem.writeVolatileLong(addrs + index * 8, addr);
    }
    
    public byte getChunkType(int index) {
        return mem.readVolatileByte(types + index * 4);
    }
    
    public void setChunkType(int index, byte type) {
        mem.writeVolatileByte(types + index * 4, type);
    }
    
    public int getChunkLength(int index) {
        return mem.readVolatileInt(lengths + index * 4);
    }
    
    public void setChunkLength(int index, int length) {
        mem.writeVolatileInt(lengths + index * 4, length);
    }
    
    /**
     * Queues a single block change to happen soon in given place.
     * @param chunk Chunk index in this buffer.
     * @param block Block index in the chunk.
     * @param newId New id for the block
     */
    public void queueChange(long chunk, long block, short newId) {
        long query = (0L << 56) | (chunk << 40) | (block << 16) | newId;
        
        changeQueue.add(query);
    }
    
    /**
     * Requests ids of a number of blocks.
     * @param chunk Index fo chunk where to operate.
     * @param indices Indices for blocks
     * @param ids Where to place ids.
     * @param beginIndex Index where to begin reading arrays.
     * @param endIndex Index before which is last index to be readed.
     */
    public void getBlocks(int chunk, int[] indices, short[] ids, int beginIndex, int endIndex) {
        ChunkFormat format = ChunkFormat.forType(getChunkType(chunk)); // Get format
        
        format.getBlocks(getChunkAddr(chunk), indices, ids, beginIndex, endIndex);
    }
    
    public short getBlock(int chunk, int index) {
        ChunkFormat format = ChunkFormat.forType(getChunkType(chunk)); // Get format
        
        return format.getBlock(getChunkAddr(chunk), index);
    }
    
    public void flushChanges() {
        changeQueue.flush();
    }
    
    /**
     * Allows building chunk buffers. One builder can create as many buffers
     * as required. Settings may be altered between building buffers, but that
     * is NOT recommended.
     *
     */
    public static class Builder {
        
        private int maxChunks;
        private int globalQueueSize;
        private int chunkQueueSize;
        private MemoryUseListener memListener;
        
        public Builder maxChunks(int maxChunks) {
            this.maxChunks = maxChunks;
            return this;
        }
        
        public Builder globalQueue(int size) {
            this.globalQueueSize = size;
            return this;
        }
        
        public Builder chunkQueue(int size) {
            this.chunkQueueSize = size;
            return this;
        }
        
        public Builder memListener(MemoryUseListener listener) {
            this.memListener = listener;
            return this;
        }
        
        public ChunkBuffer build(int index) {
            return new ChunkBuffer(index, maxChunks, globalQueueSize, chunkQueueSize, memListener);
        }
    }
    
    /**
     * Attempts to load given amount of chunks from data for which there is
     * provided a memory address.
     * @param addr Address of data's start.
     * @param count How many chunks are in that data.
     */
    public void load(long addr, int count) {
        for (int i = 0; i < count; i++) {
            // Read metadata
            byte type = mem.readByte(addr);
            int length = mem.readInt(addr + 1);
            addr += 5; // To actual chunk data
            
            // Copy data that needs to be copied
            setChunkAddr(i, addr);
            setChunkType(i, type);
            setChunkLength(i, length);
            
            // Note: avoid actually copying chunk contents as it is not really necessary
            
            // Increment pointer to point to next chunk
            addr += length;
        }
    }
    
    private int getContentSize() {
        int size = 0;
        
        int count = chunkCount.get();
        for (int i = 0; i < count; i++) {
            size += getChunkLength(i);
        }
        
        return size;
    }
    
    public int getSaveSize() {
        int count = chunkCount.get();
        return getContentSize() + count * 5;
    }
    
    public void save(long addr) {
        int count = chunkCount.get();
        for (int i = 0; i < count; i++) {
            byte type = getChunkType(i);
            int len = getChunkLength(i);
            mem.writeByte(addr, type);
            mem.writeInt(addr + 1, len);
            addr += 5; // To actual data
            
            mem.copyMemory(getChunkAddr(i), addr, len); // Copy chunk contents
            addr += len; // Address to next chunk!
        }
    }
    
    /**
     * Unloads all chunks and metadata. Using this buffer while it is unloading
     * or after it has been unloaded will probably crash your JVM.
     */
    public void unload() {
        int freed = staticDataLength;
        
        // Free chunk data
        int count = chunkCount.get();
        for (int i = 0; i < count; i++) {
            int len = getChunkLength(i);
            freed += len;
            long addr = getChunkAddr(i);
            mem.freeMemory(addr, len);
        }
        
        // Free static data region
        mem.freeMemory(addrs, staticDataLength);
        
        memListener.onFree(freed); // Notify memory listener once for whole unload
    }

    public int getMemorySize() {
        return getContentSize() + staticDataLength;
    }

    public int getId() {
        return bufferId;
    }

    public Allocator getAllocator() {
        return allocator;
    }
}
