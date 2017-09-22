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
public class ChunkBuffer2 {
    
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
                    format.processQueries(mem.readVolatileLong(addrs + i * 4), queueAddr, queriesSize);
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
    
    private ChangeQueue changeQueue;
    
    public ChunkBuffer2(short id, int maxChunks, int globalQueueSize, int chunkQueueSize, MemoryUseListener memListener) {
        bufferId = id; // Set buffer id
        
        // Initialize memory blocks for metadata
        long allocLen = maxChunks * 17 + 2 * globalQueueSize + maxChunks * chunkQueueSize;
        long baseAddr = mem.allocate(allocLen);
        addrs = baseAddr; // 8 bytes per chunk
        lengths = baseAddr + 8 * maxChunks; // 4 bytes per chunk
        used = baseAddr + 12 * maxChunks; // 4 bytes per chunk
        types = baseAddr + 16 * maxChunks; // 1 byte per chunk
        
        long globalData = baseAddr + 17;
        
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
     * empty.
     * @return Index for chunk in THIS BUFFER.
     */
    public int newChunk() {
        return chunkCount.getAndIncrement();
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
    
    public void queueChange(int chunk, int block, int newId) {
        long query = (chunk << 40) & (block << 16) & newId;
        
        changeQueue.add(query);
    }
    
    public void flushChanges() {
        changeQueue.flush();
    }
}