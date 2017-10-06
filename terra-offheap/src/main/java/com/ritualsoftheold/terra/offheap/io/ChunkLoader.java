package com.ritualsoftheold.terra.offheap.io;

import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;

/**
 * Chunk loader handles loading and possible saving chunks.
 * All methods here block calling thread.
 * 
 * All methods return the buffer that was given to them after they have
 * done necessary operations on them. Note that parameter buffer also
 * reflects any changes that might have been done.
 *
 */
public interface ChunkLoader {
    
    ChunkBuffer loadChunks(int index, ChunkBuffer buf);
    
    ChunkBuffer saveChunks(int i, ChunkBuffer buf);
    
    /**
     * Counts buffers that currently exist for this loader. Note that
     * this might literally count them so performance might vary.
     * @return Number of existing buffers.
     */
    int countBuffers();
}
