package com.ritualsoftheold.terra.offheap.io;

import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;

/**
 * Chunk loader handles loading and possible saving chunks.
 * All methods here block calling thread.
 *
 */
public interface ChunkLoader {
    
    void loadChunks(short index, ChunkBuffer buf);
    
    void saveChunks(short index, ChunkBuffer buf);
}
