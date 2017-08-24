package com.ritualsoftheold.terra.offheap.chunk;

import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Unloads chunks that were thrown out of the cache.
 *
 */
public class UnloadingRemovalListener implements RemovalListener<Integer, OffheapChunk> {
    
    private static final Memory mem = OS.memory();

    @Override
    public void onRemoval(Integer key, OffheapChunk value, RemovalCause cause) {
        mem.freeMemory(value.memoryAddress(), DataConstants.CHUNK_UNCOMPRESSED);
    }

}
