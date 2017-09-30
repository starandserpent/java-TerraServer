package com.ritualsoftheold.terra.offheap.chunk;

import java.util.concurrent.atomic.AtomicInteger;

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
     * Used chunk data lengths. Pointer to data.
     */
    private long used;
    
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
    private short bufferId;
    
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
                    long query = mem.readLong(flushAddr);
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
                
                // TODO multithreaded (going to be so much "fun" with that...)
                for (int i = 0; i < beginChunkCount; i++) {
                    int queriesSize = consumed[i]; // Data consumed by queries'
                    if (queriesSize == 0) {
                        continue; // No changes here
                    }
                    
                    long queueAddr = queues + i * queueSize;
                    
                    // Get suitable chunk format, which we'll eventually use to write the data
                    ChunkFormat format = ChunkFormat.forType(mem.readVolatileByte(types + i));
                    
                    // Ask format to process queries (and hope it handles that correctly)
                    format.processQueries(mem.readVolatileLong(addrs + i * 4), mem.readVolatileInt(lengths + i * 4),
                            allocator, queueAddr, queriesSize);
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
            // Or at this moment
            setChunkUsed(chunk, length);
            
            // Deallocate (for now) old chunk
            mem.freeMemory(oldAddr, oldLength);
            memListener.onFree(oldLength);
        }
    }
    
    private Allocator allocator;
    
    private ChangeQueue changeQueue;
    
    public ChunkBuffer(short id, int maxChunks, int globalQueueSize, int chunkQueueSize, MemoryUseListener memListener) {
        bufferId = id; // Set buffer id
        
        // Initialize memory blocks for metadata
        long allocLen = maxChunks * 17 + 2 * globalQueueSize + maxChunks * chunkQueueSize;
        long baseAddr = mem.allocate(allocLen);
        addrs = baseAddr; // 8 bytes per chunk
        lengths = baseAddr + 8 * maxChunks; // 4 bytes per chunk
        used = baseAddr + 12 * maxChunks; // 4 bytes per chunk
        types = baseAddr + 16 * maxChunks; // 1 byte per chunk
        
        long globalData = baseAddr + 17 * maxChunks;
        
        // Zero/generally set memory that needs it
        mem.setMemory(baseAddr, maxChunks * 16, (byte) 0); // Zero some chunk specific data
        mem.setMemory(types, maxChunks, ChunkType.EMPTY); // Types need to be EMPTY, even if it is not 0
        mem.setMemory(globalData, 2 * globalQueueSize + maxChunks * chunkQueueSize, (byte) 0); // Zero global data
        
        // Initialize counts (max: parameter, current: 0)
        maxCount = maxChunks;
        chunkCount = new AtomicInteger(0);
        
        // Initialize global change queue
        changeQueue = new ChangeQueue(globalData, globalData + globalQueueSize, globalQueueSize);
        
        // Initialize chunk-local queues
        queueSize = chunkQueueSize;
        queues = globalData + globalQueueSize * 2;
        
        // Save ref to memory use listener and notify it
        this.memListener = memListener;
        memListener.onAllocate(allocLen);
    }
    
    /**
     * Creates a new chunk. It will not have memory address and typo of it is set to
     * empty. If new chunk cannot be created, -1 will be returned.
     * @return Index for chunk in THIS BUFFER.
     */
    public int newChunk() {
        int index = chunkCount.getAndIncrement();
        if (index >= maxCount) { // No space...
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
        return maxCount - chunkCount.get();
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
    
    public int getChunkUsed(int index) {
        return mem.readVolatileInt(used + index * 4);
    }
    
    public void setChunkUsed(int index, int length) {
        mem.writeVolatileInt(used + index * 4, length);
    }
    
    /**
     * Queues a single block change to happen soon in given place.
     * @param chunk Chunk index in this buffer.
     * @param block Block index in the chunk.
     * @param newId New id for the block
     */
    public void queueChange(int chunk, int block, short newId) {
        long query = (chunk << 40) & (block << 16) & newId;
        
        changeQueue.add(query);
    }
    
    /**
     * Requests ids of a number of blocks.
     * @param chunk Index fo chunk where to operate.
     * @param indices Indices for blocks
     * @param ids Where to place ids.
     */
    public void getBlocks(int chunk, int[] indices, short[] ids) {
        ChunkFormat format = ChunkFormat.forType(mem.readVolatileByte(types + chunk)); // Get format
        
        format.getBlocks(getChunkAddr(chunk), indices, ids);
    }
    
    public short getBlock(int chunk, int index) {
        ChunkFormat format = ChunkFormat.forType(mem.readVolatileByte(types + chunk)); // Get format
        
        return format.getBlock(getChunkAddr(chunk), index);
    }
    
    public void flushChanges() {
        changeQueue.flush();
    }
    
    public int getBufferId() {
        return bufferId;
    }
    
    /**
     * Allows building chunk buffers. One builder can create as many buffers
     * as required. Settings may be altered between building buffers, but that
     * is NOT recommended.
     *
     */
    public static class Builder {
        
        private short id;
        private int maxChunks;
        private int globalQueueSize;
        private int chunkQueueSize;
        private MemoryUseListener memListener;
        
        public Builder id(short id) {
            this.id = id;
            return this;
        }
        
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
        
        public ChunkBuffer build() {
            return new ChunkBuffer(id, maxChunks, globalQueueSize, chunkQueueSize, memListener);
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
            setChunkUsed(i, length);
            
            // Increment pointer to point to next chunk
            addr += length;
        }
    }
}
