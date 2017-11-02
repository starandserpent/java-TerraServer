package com.ritualsoftheold.terra.offheap.world;

import com.ritualsoftheold.terra.offheap.node.OffheapChunk;

/**
 * This is called when a chunk or octree is loaded.
 * Low level stuff for performance - you're supposed
 * to give the data directly to mesher or pass it to client.
 *
 */
public interface WorldLoadListener {
    
    void octreeLoaded(long addr, long groupAddr, int id, float x, float y, float z, float scale);
    
    void chunkLoaded(OffheapChunk chunk, float x, float y, float z);
}
