package com.ritualsoftheold.terra.offheap.chunk;

import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;

/**
 * Chunk storage stores chunks in their memory representation.
 *
 */
public class ChunkStorage {
    
    /**
     * Chunk storage blocks.
     */
    private Short2ObjectMap<ChunkBuffer> storageBlocks;
    
    /**
     * Chunk pointer data length.
     */
    private int pointerLength;
    
}
