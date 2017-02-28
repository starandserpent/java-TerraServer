package com.ritualsoftheold.terra.offheap.io;

/**
 * Chunk loader handles loading and possible saving chunks.
 *
 */
public interface ChunkLoader {
    
    long loadChunks(short index);
    
    void saveChunks(short index, long addr);
}
