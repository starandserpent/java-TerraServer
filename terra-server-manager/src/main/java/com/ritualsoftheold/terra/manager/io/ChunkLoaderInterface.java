package com.ritualsoftheold.terra.manager.io;

import com.ritualsoftheold.terra.manager.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.manager.node.OffheapChunk;
import com.ritualsoftheold.terra.manager.world.OffheapLoadMarker;

/**
 * Chunk loader handles loading and possible saving chunks.
 * All methods here block calling thread.
 * 
 * All methods return the buffer that was given to them after they have
 * done necessary operations on them. Note that parameter buffer also
 * reflects any changes that might have been done.
 *
 */
public interface ChunkLoaderInterface {

    void loadChunk (OffheapChunk chunk);

    OffheapChunk getChunk (float x, float y, float z, OffheapLoadMarker loadMarker);

    ChunkBuffer saveChunks(int i, ChunkBuffer buf);
}
