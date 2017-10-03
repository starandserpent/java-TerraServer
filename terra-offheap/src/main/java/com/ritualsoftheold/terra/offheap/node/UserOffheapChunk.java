package com.ritualsoftheold.terra.offheap.node;

import java.util.concurrent.atomic.AtomicIntegerArray;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;

/**
 * Variant of OffheapChunk which modifies tracker array when it is instantiated
 * and when it is (if that happens) closed.
 *
 */
public class UserOffheapChunk extends OffheapChunk {
    
    private AtomicIntegerArray trackerArray;
    private int trackerIndex;

    public UserOffheapChunk(ChunkBuffer buf, int chunkId, MaterialRegistry materialRegistry, AtomicIntegerArray trackerArray, int trackerIndex) {
        super(buf, chunkId, materialRegistry);
        this.trackerArray = trackerArray;
        this.trackerIndex = trackerIndex;
        trackerArray.incrementAndGet(trackerIndex);
    }
    
    @Override
    public void close() throws Exception {
        trackerArray.decrementAndGet(trackerIndex);
    }

}
