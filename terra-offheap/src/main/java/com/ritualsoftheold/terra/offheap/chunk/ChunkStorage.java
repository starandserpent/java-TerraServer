package com.ritualsoftheold.terra.offheap.chunk;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Stores chunk data in offheap memory.
 *
 */
public class ChunkStorage {
    
    private static Memory mem = OS.memory();
    
    /**
     * The beginning of chunk storage.
     */
    private long address;
    
    /**
     * Length of chunk storage.
     */
    private long length;
    
    public ChunkStorage() {
        
    }
}
