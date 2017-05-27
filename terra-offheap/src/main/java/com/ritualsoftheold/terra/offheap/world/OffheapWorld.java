package com.ritualsoftheold.terra.offheap.world;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.node.Chunk;
import com.ritualsoftheold.terra.node.Octree;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.ChunkStorage;
import com.ritualsoftheold.terra.offheap.io.ChunkLoader;
import com.ritualsoftheold.terra.offheap.io.OctreeLoader;
import com.ritualsoftheold.terra.offheap.node.OffheapOctree;
import com.ritualsoftheold.terra.offheap.octree.OctreeStorage;
import com.ritualsoftheold.terra.world.TerraWorld;
import com.ritualsoftheold.terra.world.gen.WorldGenerator;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Represents world that is mainly backed by offheap memory.
 */
public class OffheapWorld implements TerraWorld {
    
    private static final Memory mem = OS.memory();
    
    // Loaders/savers
    private ChunkLoader chunkLoader;
    private OctreeLoader octreeLoader;
    
    // Data storage
    private Executor storageExecutor;
    private ChunkStorage chunkStorage;
    private OctreeStorage octreeStorage;
    
    // Some cached stuff
    private OffheapOctree masterOctree;
    private float masterScale;
    
    // World generation
    private WorldGenerator generator;
    private Executor generatorExecutor;
    
    private MaterialRegistry registry;
    
    public OffheapWorld(ChunkLoader chunkLoader, OctreeLoader octreeLoader, MaterialRegistry registry) {
        this.chunkLoader = chunkLoader;
        this.octreeLoader = octreeLoader;
        
        // Init storages
        this.storageExecutor = new ForkJoinPool();
        this.chunkStorage = new ChunkStorage(chunkLoader, storageExecutor, 64, 1024); // TODO settings
        this.octreeStorage = new OctreeStorage(8192, octreeLoader, storageExecutor);
        
        this.registry = registry;
    }

    @Override
    public Octree getMasterOctree() {
        return masterOctree;
    }

    @Override
    public MaterialRegistry getMaterialRegistry() {
        return registry;
    }

    @Override
    public Chunk getChunk(float x, float y, float z) {
        return null; // TODO
    }
    
    /**
     * Attempts to get an id for smallest node at given coordinates.
     * @param x X coordinate.
     * @param y Y coordinate.
     * @param z Z coordinate.
     * @return 32 least significant bits represent the actual id. 33th
     * tells if the id refers to chunk (1) or octree (2).
     */
    private long getNodeId(float x, float y, float z) {
        long addr = masterOctree.memoryAddress(); // Get starting memory address
        float scale = masterScale; // Starting scale
        
        float octreeX = 0, octreeY = 0, octreeZ = 0;
        while (true) {
            // Adjust the coordinates to be relative to current octree
            x -= octreeX;
            y -= octreeY;
            z -= octreeZ;
            
            // We octree position this much to get center of new octree
            float posMod = 0.25f * scale;
            
            // Octree index, determined by lookup table below
            int index = 0;
            if (x < 0) {
                octreeX -= posMod;
                
                if (y < 0) {
                    octreeY -= posMod;
                    
                    if (z < 0) {
                        octreeZ -= posMod;
                        index = 0;
                    } else {
                        octreeZ += posMod;
                        index = 4;
                    }
                } else {
                    octreeY += posMod;
                    
                    if (z < 0) {
                        octreeZ -= posMod;
                        index = 2;
                    } else {
                        octreeZ += posMod;
                        index = 6;
                    }
                }
            } else {
                octreeX += posMod;
                
                if (y < 0) {
                    octreeY -= posMod;
                    
                    if (z < 0) {
                        octreeZ -= posMod;
                        index = 1;
                    } else {
                        octreeZ += posMod;
                        index = 5;
                    }
                } else {
                    octreeY += posMod;
                    
                    if (z < 0) {
                        octreeZ -= posMod;
                        index = 3;
                    } else {
                        octreeZ += posMod;
                        index = 7;
                    }
                }
            }
            
            int entry = mem.readInt(addr + 1 + index); // Read octree entry
            boolean isOctree = mem.readByte(addr) >>> index == 1; // Get flags, check this index against them
            scale *= 0.5f; // Halve the scale, we are moving to child node
            
            if (scale < DataConstants.CHUNK_SCALE + 1) { // Found a chunk
                if (isOctree && entry == 0) { // Chunk-null: needs to be created
                    entry = handleGenerate(x, y, z); // World gen here
                    mem.writeInt(addr + 1 + index, entry);
                }
                
                return 1 << 32 | entry;
            } else { // Just octree or single block here
                if (isOctree && entry == 0) { // Octree-null: needs to be created
                    entry = octreeStorage.newOctree();
                    mem.writeInt(addr + 1 + index, entry);
                }
                
                if (isOctree) {
                    long groupAddr = octreeStorage.getGroup((byte) (entry >>> 24));
                    addr = groupAddr + (index & 0xffffff) * DataConstants.OCTREE_SIZE; // Update address to point to new octree
                } else {
                    return entry;
                }
            }
        }
    }
    
    @Override
    public CompletableFuture<Octree> requestOctree(int index) {
        byte groupIndex = (byte) (index >>> 24);
        int octreeIndex = index & 0xffffff;
        
        CompletableFuture<Octree> future = CompletableFuture.supplyAsync(() -> {
            long groupAddr = octreeStorage.getGroup(groupIndex);
            // This future will block on groupFuture.get()... hopefully
            long addr = groupAddr + octreeIndex * DataConstants.OCTREE_SIZE;
            
            OffheapOctree octree = new OffheapOctree(this, 0f); // TODO scale, somehow
            octree.memoryAddress(addr); // Validate octree with memory address!
            
            return octree;
        }, storageExecutor);
        return future;
    }
    
    public int handleGenerate(float x, float y, float z) {
        short[] data = new short[DataConstants.CHUNK_MAX_BLOCKS];
        generator.generate(data, x, y, z, DataConstants.CHUNK_SCALE);
        
        long tempAddr = mem.allocate(DataConstants.CHUNK_UNCOMPRESSED);
        
        // TODO memory copy is faster (but not as safe)
        for (int i = 0; i < data.length; i++) {
            mem.writeShort(tempAddr + i * 2, data[i]);
        }
        
        int chunkId = chunkStorage.addChunk(tempAddr, registry);
        return chunkId;
    }
    
    @Override
    public CompletableFuture<Chunk> requestChunk(int index) {
        return chunkStorage.requestChunk(index, getMaterialRegistry());
    }
    
    public OctreeStorage getOctreeStorage() {
        return octreeStorage;
    }
    
    public ChunkStorage getChunkStorage() {
        return chunkStorage;
    }

}
