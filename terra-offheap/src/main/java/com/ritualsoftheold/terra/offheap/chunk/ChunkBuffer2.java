package com.ritualsoftheold.terra.offheap.chunk;

import java.util.concurrent.atomic.AtomicInteger;

import com.ritualsoftheold.terra.offheap.memory.MemoryUseListener;

/**
 * Contains chunks in memory.
 *
 */
public class ChunkBuffer2 {
    
    /**
     * Chunk memory addresses.
     */
    private long[] addrs;
    
    /**
     * Chunk types. Note that not all chunk types have associated address!
     */
    private int[] types;
    
    /**
     * Current count of chunks in this buffer. This also serves
     * as first free index to this buffer.
     */
    private int chunkCount;
    
    /**
     * Maximum count of chunks in this buffer.
     */
    private AtomicInteger maxCount;
    
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
    
    /**
     * Creates a new chunk. It will not have memory address and typo of it is set to
     * empty.
     * @return Index for chunk in THIS BUFFER.
     */
    public int newChunk() {
        return maxCount.getAndIncrement();
    }
    
    public long getChunkAddr(int index) {
        return addrs[index];
    }
    
    public void setChunkAddr(int index, long addr) {
        addrs[index] = addr;
    }
    
    public int getChunkType(int index) {
        return types[index];
    }
    
    public void setChunkType(int type) {
        types[type] = type;
    }
    
    public void queueChange(int chunk, int block, int newId) {
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
         * Queries are to be manually flushed periodically. When flushing,
         * all queries that were submitted when flushing started will be
         * written into actual data. At end of flushing operation, any
         * new queries will be moved to start.
         * 
         * After that, queue will be validated to ensure that there was no
         * race condition. All queries found after initial start will be
         * re-submitted.
         */
        
        // TODO
    }
}
