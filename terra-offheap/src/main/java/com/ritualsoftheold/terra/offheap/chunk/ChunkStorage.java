package com.ritualsoftheold.terra.offheap.chunk;

import java.util.HashSet;
import java.util.Set;

import com.ritualsoftheold.terra.offheap.node.OffheapChunk;

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
}
