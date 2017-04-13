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
    
    ChunkBuffer loadChunks(short index, ChunkBuffer buf);
    
    ChunkBuffer saveChunks(short index, ChunkBuffer buf);
}
