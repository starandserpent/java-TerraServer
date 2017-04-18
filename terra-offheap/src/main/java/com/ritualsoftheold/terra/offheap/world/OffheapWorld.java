package com.ritualsoftheold.terra.offheap.world;

import java.io.InterruptedIOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.node.Chunk;
import com.ritualsoftheold.terra.node.Octree;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.ChunkStorage;
import com.ritualsoftheold.terra.offheap.io.ChunkLoader;
import com.ritualsoftheold.terra.offheap.io.OctreeLoader;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;
import com.ritualsoftheold.terra.offheap.node.OffheapOctree;
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
    private Executor storageExecutor;
    private ChunkStorage chunkStorage;
    private OctreeStorage octreeStorage;
    
    // Some cached stuff
    private OffheapOctree masterOctree;
    
    public OffheapWorld(ChunkLoader chunkLoader, OctreeLoader octreeLoader) {
        this.chunkLoader = chunkLoader;
        this.octreeLoader = octreeLoader;
        
        // Init storages
        this.storageExecutor = new ForkJoinPool();
        this.chunkStorage = new ChunkStorage(chunkLoader, storageExecutor, 64, 1024); // TODO settings
        this.octreeStorage = new OctreeStorage(8192, octreeLoader, storageExecutor);
        
        // Caching...
    }

    @Override
    public Octree getMasterOctree() {
        return masterOctree;
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
    
    @Override
    public CompletableFuture<Octree> requestOctree(int index) {
        byte groupIndex = (byte) (index >>> 24);
        int octreeIndex = index & 0xffffff;
        
        CompletableFuture<Long> groupFuture = octreeStorage.requestGroup(groupIndex);
        CompletableFuture<Octree> future = CompletableFuture.supplyAsync(() -> {
            // This future will block on groupFuture.get()... hopefully
            long addr = groupFuture.join() + octreeIndex * DataConstants.OCTREE_SIZE;
            
            OffheapOctree octree = new OffheapOctree(this, 0f); // TODO scale, somehow
            octree.memoryAddress(addr); // Validate octree with memory address!
            
            return octree;
        }, storageExecutor);
        return future;
    }
    
    public CompletableFuture<Chunk> requestChunk(int index) {
        return chunkStorage.requestChunk(index, getMaterialRegistry());
    }

}
