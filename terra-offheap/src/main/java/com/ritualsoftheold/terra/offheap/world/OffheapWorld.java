package com.ritualsoftheold.terra.offheap.world;

import java.util.Set;
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
import com.ritualsoftheold.terra.world.LoadMarker;
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
    
    // Load markers
    private Set<LoadMarker> loadMarkers;
    
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
    
    /**
     * Loads all nodes which are around given coordinates but within
     * given radius. Used for implementing load markers.
     * @param x
     * @param y
     * @param z
     * @param radius
     */
    private void loadArea(float x, float y, float z, float radius) {
        long addr = masterOctree.memoryAddress(); // Get starting memory address
        
        float scale = masterScale; // Starting scale
        int entry = 0; // Chunk or octree id
        boolean isOctree = true; // If the final entry is chunk or octree
        
        float octreeX = 0, octreeY = 0, octreeZ = 0;
        while (true) {
            if (radius > scale * scale) {
                // We found small enough unit... load everything inside it!
                break;
            }
            
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
            
            entry = mem.readInt(addr + 1 + index * DataConstants.OCTREE_NODE_SIZE); // Read octree entry
            isOctree = (mem.readByte(addr) >>> index & 1) == 1; // Get flags, check this index against them
            scale *= 0.5f; // Halve the scale, we are moving to child node
            
            if (scale < DataConstants.CHUNK_SCALE + 1) { // Found a chunk
                if (isOctree && entry == 0) { // Chunk-null: needs to be created
                    entry = handleGenerate(x, y, z); // World gen here
                    mem.writeInt(addr + 1 + index * DataConstants.OCTREE_NODE_SIZE, entry);
                }
                
                break;
            } else { // Just octree or single block here
                if (isOctree && entry == 0) { // Octree-null: needs to be created
                    entry = octreeStorage.newOctree();
                    mem.writeInt(addr + 1 + index * DataConstants.OCTREE_NODE_SIZE, entry);
                }
                
                if (isOctree) {
                    long groupAddr = octreeStorage.getGroup((byte) (entry >>> 24));
                    addr = groupAddr + (index & 0xffffff) * DataConstants.OCTREE_SIZE; // Update address to point to new octree
                } else {
                    break;
                }
            }
        }
        
        // If we needed to only load one chunk, that is done already
        if (!isOctree) {
            return;
        }
        
        loadAll(entry, scale, x, y, z);
    }
    
    /**
     * Loads all children of given octree index.
     * @param index
     */
    public void loadAll(int index, float scale, float x, float y, float z) {
        long groupAddr = octreeStorage.getGroup((byte) (index >>> 24));
        long addr = groupAddr + (index & 0xffffff) * DataConstants.OCTREE_SIZE; // Update address to point to new octree
        
        byte flags = mem.readByte(addr);
        float childScale = scale * 0.5f;
        
        // TODO check if I need to modify this to be a tail call manually (does the stack overflow?)
        if (childScale == DataConstants.CHUNK_SCALE) { // Nodes might be chunks
            for (int i = 0; i < 8; i++) {
                if ((flags >>> i & 1) == 1) { // Octree, we need to make sure it is loaded
                    int entry = mem.readInt(addr + 1 + i * DataConstants.OCTREE_NODE_SIZE);
                    
                    if (entry == 0) { // Ooops, need to generate a chunk now
                        entry = handleGenerate(x, y, z); // World gen here
                        mem.writeInt(addr + 1 + index * DataConstants.OCTREE_NODE_SIZE, entry);
                    } else {
                        // TODO ensure that chunk is loaded
                    }
                }
            }
        } else { // Nodes might be octrees
            float posMod = 0.25f * scale;
            for (int i = 0; i < 8; i++) {
                if ((flags >>> i & 1) == 1) { // Octree, we need to make sure it is loaded
                    int entry = mem.readInt(addr + 1 + i * DataConstants.OCTREE_NODE_SIZE);
                    
                    if (entry == 0) { // Oops, it doesn't exist. Create it!
                        entry = octreeStorage.newOctree();
                        mem.writeInt(addr + 1 + index * DataConstants.OCTREE_NODE_SIZE, entry);
                    }
                    
                    // Create positions for subnodes
                    float x2 = x, y2 = y, z2 = z;
                    switch (i) {
                    case 0:
                        x2 -= posMod;
                        y2 -= posMod;
                        z2 -= posMod;
                        break;
                    case 1:
                        x2 += posMod;
                        y2 -= posMod;
                        z2 -= posMod;
                        break;
                    case 2:
                        x2 -= posMod;
                        y2 += posMod;
                        z2 -= posMod;
                        break;
                    case 3:
                        x2 += posMod;
                        y2 += posMod;
                        z2 -= posMod;
                        break;
                    case 4:
                        x2 -= posMod;
                        y2 -= posMod;
                        z2 += posMod;
                        break;
                    case 5:
                        x2 += posMod;
                        y2 -= posMod;
                        z2 += posMod;
                        break;
                    case 6:
                        x2 -= posMod;
                        y2 += posMod;
                        z2 += posMod;
                        break;
                    case 7:
                        x2 += posMod;
                        y2 += posMod;
                        z2 += posMod;
                        break;
                    }
                    
                    loadAll(entry, childScale, x2, y2, z2); // Recursively load that too
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

    @Override
    public void addLoadMarker(LoadMarker marker) {
        // TODO Auto-generated method stub
        
    }

}
