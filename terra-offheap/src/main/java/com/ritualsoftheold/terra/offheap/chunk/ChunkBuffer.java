package com.ritualsoftheold.terra.offheap.chunk;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Represents a buffer or "block" of chunks.
 *
 */
public class ChunkBuffer {
    
    private static Memory mem = OS.memory();
    
    /**
     * Address to chunk pointer data.
     */
    private long pointerAddr;
    
    /**
     * Maximum chunk count in this buffer.
     */
    private int chunkCount;
    
    /**
     * Array of memory addresses for chunks.
     */
    private long[] chunks;
    
    /**
     * Array of chunk data lengths.
     */
    private int[] lengths;
    
    /**
     * Index of first free chunk slot in this buffer.
     */
    private int freeIndex;
    
    public ChunkBuffer(int chunkCount) {
        this.chunkCount = chunkCount;
        
        chunks = new long[chunkCount];
        lengths = new int[chunkCount];
        freeIndex = 0;
    }
    
    /**
     * Checks if this buffer can take one more chunk.
     * @return If one more chunk can be added.
     */
    public boolean hasSpace() {
        return chunkCount > freeIndex;
    }
    
    /**
     * Creates a chunk to this buffer.
     * @param firstLength Starting length of chunk (in memory). Try to have
     * something sensible here to avoid instant reallocation.
     * @return Chunk index in this buffer.
     */
    public int createChunk(int firstLength) {
        // Take index, adjust freeIndex
        int index = freeIndex;
        freeIndex++;
        
        long addr = mem.allocate(firstLength);
        chunks[index] = addr;
        lengths[index] = firstLength;
        
        // And finally return the index
        return index;
    }
    
    /**
     * Reallocates chunk with given amount of space. Note that you <b>must</b>
     * free old data, after you have copied all relevant parts to new area.
     * @param index Chunk index in this buffer.
     * @param newLength New length in bytes.
     * @return New memory address. Remember to free the old one!
     */
    public long reallocChunk(int index, int newLength) {
        long addr = mem.allocate(newLength);
        
        chunks[index] = addr;
        lengths[index] = newLength;
        
        return addr;
    }
}
