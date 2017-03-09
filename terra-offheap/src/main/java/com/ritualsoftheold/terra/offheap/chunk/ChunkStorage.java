package com.ritualsoftheold.terra.offheap.chunk;

import it.unimi.dsi.fastutil.ints.Int2LongMap;

/**
 * Chunk storage stores chunks in their memory representation.
 * 
 * For now this doesn't use ChunkBuffers. I truly hope that it
 * works well enough.
 *
 */
public class ChunkStorage {
    
    /**
     * A map of chunk addresses.
     */
    private Int2LongMap chunkMap;
    
}
