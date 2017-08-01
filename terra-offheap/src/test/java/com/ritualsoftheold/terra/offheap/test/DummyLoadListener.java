package com.ritualsoftheold.terra.offheap.test;

import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.world.WorldLoadListener;

/**
 * Ignores data.
 *
 */
public class DummyLoadListener implements WorldLoadListener {

    @Override
    public void octreeLoaded(long addr, long groupAddr, float x, float y,
            float z, float scale) {
        
    }

    @Override
    public void chunkLoaded(long addr, ChunkBuffer buf, float x, float y,
            float z) {
        
    }

}
