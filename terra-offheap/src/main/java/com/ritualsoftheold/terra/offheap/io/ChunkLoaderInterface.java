package com.ritualsoftheold.terra.offheap.io;

import com.ritualsoftheold.terra.core.gen.objects.LoadMarker;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;
import com.ritualsoftheold.terra.offheap.world.OffheapLoadMarker;
import com.ritualsoftheold.terra.offheap.world.WorldLoadListener;

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

    OffheapChunk getChunk (float x, float z, OffheapLoadMarker loadMarker);

    ChunkBuffer saveChunks(int i, ChunkBuffer buf);
}
