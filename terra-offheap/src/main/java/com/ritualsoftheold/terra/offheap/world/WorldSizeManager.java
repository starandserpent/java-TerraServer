package com.ritualsoftheold.terra.offheap.world;

import com.ritualsoftheold.terra.offheap.octree.OctreeStorage;

/**
 * Handles enlarging master octree, which is quite complex operation.
 *
 */
public class WorldSizeManager {
    
    private OffheapWorld world;
    private OctreeStorage storage;
    
    public WorldSizeManager(OffheapWorld world) {
        this.world = world;
        this.storage = world.getOctreeStorage();
    }
}
