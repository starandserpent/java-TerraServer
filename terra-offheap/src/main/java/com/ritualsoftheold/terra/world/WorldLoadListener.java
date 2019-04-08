package com.ritualsoftheold.terra.world;

import com.ritualsoftheold.terra.gen.objects.LoadMarker;
import com.ritualsoftheold.terra.Pointer;
import com.ritualsoftheold.terra.node.OffheapChunk;

/**
 * Methods in in this interface will be called when world data is loaded.
 *
 */
public interface WorldLoadListener {
    
    /**
     * This method is called when an octree is loaded to storage.
     * @param address Address of octree that was loaded.
     * @param groupAddr Address of an octree group where the octree
     * was loaded to.
     * @param id Full id of the octree.
     * @param x X coordinate of center of the octree.
     * @param y Y coordinate of center of the octree.
     * @param z Z coordinate of center of the octree.
     * @param scale Scale of the octree.
     * @param trigger Load marker that triggered this operation or null.
     */
    void octreeLoaded(@Pointer long addr, @Pointer long groupAddr, int id, float x, float y, float z, float scale, LoadMarker trigger);
    
    /**
     * This method is called when a chunk is loaded to storage.
     * @param chunk Temporary on-heap wrapper of the chunk. When this method
     * returns, it is not safe to use anymore.
     * @param x X coordinate of the chunk.
     * @param y Y coordinate of the chunk
     * @param z Z coordinate of the chunk.
     * @param trigger Load marker that triggered this operation or null.
     */
    void chunkLoaded(OffheapChunk chunk, float x, float y, float z, LoadMarker trigger);
    
    /**
     * Called when loading world is finished.
     * @param trigger Load marker that originally triggered the loading that
     * is now completed.
     */
    default void finished(LoadMarker trigger) {
        
    }
}
