package com.ritualsoftheold.terra.offheap.test;

import com.ritualsoftheold.terra.offheap.node.OffheapChunk;
import com.ritualsoftheold.terra.offheap.world.WorldLoadListener;
import com.ritualsoftheold.terra.world.LoadMarker;

/**
 * Ignores data.
 *
 */
public class DummyLoadListener implements WorldLoadListener {

    @Override
    public void octreeLoaded(long addr, long groupAddr, int id, float x,
            float y, float z, float scale, LoadMarker trigger) {
        // Auto-generated method stub
        
    }

    @Override
    public void chunkLoaded(OffheapChunk chunk, float x, float y, float z, LoadMarker trigger) {
        // Auto-generated method stub
        
    }

}
