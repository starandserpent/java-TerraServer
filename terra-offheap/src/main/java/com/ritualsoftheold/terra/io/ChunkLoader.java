package com.ritualsoftheold.terra.io;

import com.ritualsoftheold.terra.chunk.ChunkBuffer;

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
}
