package com.ritualsoftheold.terra.offheap.chunk;

import java.util.HashSet;
import java.util.Set;

import com.ritualsoftheold.terra.node.Chunk;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Stores chunk data in offheap memory.
 *
 */
public class ChunkStorage {
    
    private static Memory mem = OS.memory();
    
    private Set<OffheapChunk> chunks;
    
    public ChunkStorage() {
        chunks = new HashSet<>();
    }
    
    /**
     * Forces reallocation of given chunk's block storage.
     * @param chunk
     * @param length
     */
    public void alloc(OffheapChunk chunk, int length) {
        
    }
}
