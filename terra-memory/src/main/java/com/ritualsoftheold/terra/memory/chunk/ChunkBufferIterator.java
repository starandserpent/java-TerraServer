package com.ritualsoftheold.terra.memory.chunk;

/**
 * Allows iterating over chunk with while loop.
 *
 */
public class ChunkBufferIterator {

    /**
     * Array of memory addresses for chunks.
     */
    private long[] chunks;
    
    /**
     * Array of chunk data lengths.
     */
    private long[] lengths;
    
    /**
     * Iterator index.
     */
    private int index;
    
    /**
     * Index of last chunk in this buffer.
     */
    private int lastChunk;
    
    ChunkBufferIterator(long[] chunks, long[] lengths, int lastChunk) {
        this.chunks = chunks;
        this.lengths = lengths;
        this.index = -1;
        this.lastChunk = lastChunk;
    }
    
    /**
     * Advances buffer to next chunk, if possible.
     * @return If advancement succeeded
     */
    public boolean next() {
        if (index >= lastChunk) {
            // Fail, out of chunks!
            return false;
        }
        index++;
        
        return true;
    }
    
    public long getAddress() {
        return chunks[index];
    }
    
    public long getLength() {
        return lengths[index];
    }
}
