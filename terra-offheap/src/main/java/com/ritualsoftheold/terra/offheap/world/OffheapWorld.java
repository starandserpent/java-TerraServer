package com.ritualsoftheold.terra.offheap.world;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.node.Chunk;
import com.ritualsoftheold.terra.node.Octree;
import com.ritualsoftheold.terra.offheap.chunk.ChunkStorage;
import com.ritualsoftheold.terra.offheap.io.ChunkLoader;
import com.ritualsoftheold.terra.offheap.io.OctreeLoader;
import com.ritualsoftheold.terra.offheap.octree.OctreeStorage;
import com.ritualsoftheold.terra.world.TerraWorld;

/**
 * Represents world that is mainly backed by offheap memory.
 */
public class OffheapWorld implements TerraWorld {
    
    // Loaders/savers
    private ChunkLoader chunkLoader;
    private OctreeLoader octreeLoader;
    
    // Data storage
    private ChunkStorage chunkStorage;
    private OctreeStorage octreeStorage;
    
    public OffheapWorld(ChunkLoader chunkLoader, OctreeLoader octreeLoader) {
        this.chunkLoader = chunkLoader;
        this.octreeLoader = octreeLoader;
        
        // Init storages
        this.chunkStorage = new ChunkStorage(chunkLoader, 10, 64, 1024); // TODO settings
        this.octreeStorage = new OctreeStorage(8192, octreeLoader, 10);
        
    }

    @Override
    public Octree getMasterOctree() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MaterialRegistry getMaterialRegistry() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Chunk getChunk(float x, float y, float z) {
        // TODO Auto-generated method stub
        return null;
    }

}
