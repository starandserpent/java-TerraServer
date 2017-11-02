package com.ritualsoftheold.terra.offheap.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.node.Chunk;
import com.ritualsoftheold.terra.node.Node;
import com.ritualsoftheold.terra.node.Octree;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkStorage;
import com.ritualsoftheold.terra.offheap.chunk.ChunkType;
import com.ritualsoftheold.terra.offheap.io.ChunkLoader;
import com.ritualsoftheold.terra.offheap.io.OctreeLoader;
import com.ritualsoftheold.terra.offheap.memory.MemoryManager;
import com.ritualsoftheold.terra.offheap.memory.MemoryPanicHandler;
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
    private List<LoadMarker> loadMarkers;
    private WorldLoadListener loadListener;
    
    // Memory management
    private MemoryManager memManager;
    
    private WorldSizeManager sizeManager;
    
    private float centerX;
    private float centerY;
    private float centerZ;
    
    public static class Builder {
        
        private OffheapWorld world;
        
        private int octreeGroupSize;
        
        private long memPreferred;
        private long memMax;
        private MemoryPanicHandler memPanicHandler;
        
        private ChunkBuffer.Builder chunkBufferBuilder;
        private int chunkMaxBuffers;
        
        public Builder() {
            world = new OffheapWorld();
        }
        
        public Builder chunkLoader(ChunkLoader loader) {
            world.chunkLoader = loader;
            return this;
        }
        
        public Builder octreeLoader(OctreeLoader loader) {
            world.octreeLoader = loader;
            return this;
        }
        
        public Builder storageExecutor(Executor executor) {
            world.storageExecutor = executor;
            return this;
        }
        
        public Builder chunkStorage(ChunkBuffer.Builder bufferBuilder, int maxBuffers) {
            this.chunkBufferBuilder = bufferBuilder;
            this.chunkMaxBuffers = maxBuffers;
            
            return this;
        }
        
        public Builder octreeStorage(int groupSize) {
            this.octreeGroupSize = groupSize;
            return this;
        }
        
        public Builder generator(WorldGenerator generator) {
            world.generator = generator;
            return this;
        }
        
        public Builder generatorExecutor(Executor executor) {
            world.generatorExecutor = executor;
            return this;
        }
        
        public Builder materialRegistry(MaterialRegistry registry) {
            world.registry = registry;
            return this;
        }
        
        public Builder memorySettings(long preferred, long max, MemoryPanicHandler panicHandler) {
            this.memPreferred = preferred;
            this.memMax = max;
            this.memPanicHandler = panicHandler;
            
            return this;
        }
        
        public OffheapWorld build() {
            // Initialize some internal structures AFTER all user-controller initialization
            world.loadMarkers = new ArrayList<>();
            world.sizeManager = new WorldSizeManager(world);
            
            // Initialize memory manager
            world.memManager = new MemoryManager(world, memPreferred, memMax, memPanicHandler);
            
            // Initialize stuff that needs memory manager
            world.octreeStorage = new OctreeStorage(octreeGroupSize, world.octreeLoader, world.storageExecutor, world.memManager);
            chunkBufferBuilder.memListener(world.memManager);
            world.chunkStorage = new ChunkStorage(chunkBufferBuilder, chunkMaxBuffers, world.chunkLoader, world.storageExecutor);
            
            // Update master octree
            world.updateMasterOctree();
            
            return world;
        }
    }
    
    // Only used by the builder
    private OffheapWorld() {
        
    }

    @Override
    public Octree getMasterOctree() {
        return masterOctree;
    }

    @Override
    public MaterialRegistry getMaterialRegistry() {
        return registry;
    }
    
    public Node getNode(float x, float y, float z) {
        long nodeData = getNodeId(x, y, z);
        boolean isChunk = nodeData >>> 32 == 1;
        int nodeId = (int) (nodeData & 0xffffff);
        
        if (isChunk) {
            return chunkStorage.getChunk(nodeId, registry);
        } else {
            return octreeStorage.getOctree(nodeId, registry);
        }
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
                } else {
                    chunkStorage.ensureLoaded(entry);
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
     * @param listener
     * @param noGenerate
     */
    public void loadArea(float x, float y, float z, float radius, WorldLoadListener listener, boolean noGenerate) {
        // Sanity checks for debugging
        assert radius != 0;
        assert listener != null;
        
        long addr = masterOctree.memoryAddress(); // Get starting memory address
        int nodeId = 0;
        System.out.println("addr: " + addr);
        listener.octreeLoaded(addr, octreeStorage.getGroup(0), nodeId, x, y, z, masterScale);
        
        float scale = masterScale; // Starting scale
        boolean isOctree = true; // If the final entry is chunk or octree
        
        // Max/min coordinates    
        float farX = radius + x;
        float farY = radius + y;
        float farZ = radius + z;
        float nearX = x - radius;
        float nearY = y - radius;
        float nearZ = z - radius;
        
        float posMod = 0.25f * scale;
        byte extend = (byte) 0b11111111;
        
        if (farX > centerX + posMod) { // RIGHT
            extend &= 0b10101010;
        }
        if (farY > centerY + posMod) { // UP
            extend &= 0b11001100;
        }
        if (farZ > centerY + posMod) { // BACK
            extend &= 0b11110000;
        }
        if (nearX < centerX - posMod) { // LEFT
            extend &= 0b01010101;
        }
        if (nearY < centerY - posMod) { // DOWN
            extend &= 0b00110011;
        }
        if (nearZ < centerZ - posMod) { // FRONT
            extend &= 0b00001111;
        }
        
        if (extend != 0b11111111) { // We need to enlarge the world
            System.out.println("Need to enlarge master octree: " + Integer.toBinaryString(extend));
            if (extend == 0) { // oops, no direction satisfies, panic mode
                // TODO do something sensible here instead of just throwing zero
                sizeManager.queueEnlarge(2 * scale, 0);
            } else {
                for (int i = 7; i >= 0; i--) {
                    if ((extend >>> i & 1) == 1) {
                        System.out.println("Queue enlarge for " + i);
                        sizeManager.queueEnlarge(2 * scale, i);
                        break;
                    }
                }
            }
        }
        
        long groupAddr = octreeStorage.getMasterGroupAddr();
        
        float octreeX = centerX, octreeY = centerY, octreeZ = centerZ;
        
        // Bugged, TODO fix it
//        while (true) {
//            // Adjust the coordinates to be relative to current octree
//            x -= octreeX;
//            y -= octreeY;
//            z -= octreeZ;
//            
//            // We octree position this much to get center of new octree
//            posMod = 0.25f * scale;
//            
//            // Octree index, determined by lookup table below
//            int index = 0;
//            if (x < 0) {
//                octreeX -= posMod;
//                
//                if (y < 0) {
//                    octreeY -= posMod;
//                    
//                    if (z < 0) {
//                        octreeZ -= posMod;
//                        index = 0;
//                    } else {
//                        octreeZ += posMod;
//                        index = 4;
//                    }
//                } else {
//                    octreeY += posMod;
//                    
//                    if (z < 0) {
//                        octreeZ -= posMod;
//                        index = 2;
//                    } else {
//                        octreeZ += posMod;
//                        index = 6;
//                    }
//                }
//            } else {
//                octreeX += posMod;
//                
//                if (y < 0) {
//                    octreeY -= posMod;
//                    
//                    if (z < 0) {
//                        octreeZ -= posMod;
//                        index = 1;
//                    } else {
//                        octreeZ += posMod;
//                        index = 5;
//                    }
//                } else {
//                    octreeY += posMod;
//                    
//                    if (z < 0) {
//                        octreeZ -= posMod;
//                        index = 3;
//                    } else {
//                        octreeZ += posMod;
//                        index = 7;
//                    }
//                }
//            }
//            
//            if (farX > octreeX + posMod) { // RIGHT
//                break;
//            }
//            if (farY > octreeY + posMod) { // UP
//                break;
//            }
//            if (farZ > octreeZ + posMod) { // BACK
//                break;
//            }
//            if (nearX < octreeX - posMod) { // LEFT
//                break;
//            }
//            if (nearY < octreeY - posMod) { // DOWN
//                break;
//            }
//            if (nearZ < octreeZ - posMod) { // FRONT
//                break;
//            }
//            
//            // I hope volatile is enough to avoid race conditions
//            System.out.println("Read flags: " + addr);
//            isOctree = (mem.readVolatileByte(addr) >>> index & 1) == 1; // Get flags, check this index against them
//            scale *= 0.5f; // Halve the scale, we are moving to child node
//            
//            long nodeAddr = addr + 1 + index * DataConstants.OCTREE_NODE_SIZE; // Get address of the node
//            
//            if (scale < DataConstants.CHUNK_SCALE + 1) { // Found a chunk
//                // Check if there is a chunk; if not, generate one
//                if (mem.readVolatileInt(nodeAddr) == 0) {
//                    // TODO
//                    System.out.println("TODO here!");
//                }
//                
//                break;
//            } else { // Just octree or single block here
//                if (isOctree) {
//                    // Check if there is an octree; if not, create one
//                    System.out.println("Octree: " + mem.readVolatileInt(nodeAddr));
//                    if (mem.readVolatileInt(nodeAddr) == 0 && !noGenerate) {
//                        int octreeIndex = octreeStorage.newOctree(); // Allocate new octree
//                        System.out.println("CAS octree: " + (octreeIndex >>> 24));
//                        boolean success = mem.compareAndSwapInt(nodeAddr, 0, octreeIndex); // if no one else allocated it yet, save index
//                        System.out.println("CAS result: " + success);
//                        // This creates empty octrees, but probably not often
//                    }
//                    groupAddr = octreeStorage.getGroup(mem.readVolatileByte(nodeAddr)); // First byte of octree node is group addr!
//                    System.out.println("groupIndex: " + mem.readVolatileByte(nodeAddr) + ", groupAddr: " + groupAddr);
//                    
//                    addr = groupAddr + (mem.readVolatileInt(nodeAddr) >>> 8) * DataConstants.OCTREE_SIZE; // Update address to point to new octree
//                    
//                    listener.octreeLoaded(addr, groupAddr, octreeX, octreeY, octreeZ, scale);
//                } else {
//                    System.out.println("Single node: " + scale);
//                    break;
//                }
//            }
//        }
        
        // If we needed to only load one chunk, that is done already
        if (!isOctree) {
            return;
        }
        
        // Prepare to loadAll, then join all remaining futures here
        List<CompletableFuture<Void>> futures = new ArrayList<>((int) (scale / 16 * scale / 16 * scale / 16) + 10); // Assume size of coming futures
        loadAll(groupAddr, addr, nodeId, scale, octreeX, octreeY, octreeZ, listener, futures, noGenerate);
        for (CompletableFuture<Void> future : futures) { // Join all futures now
            future.join();
        }
    }
    
    /**
     * Starts loading all children of given octree index.
     */
    private void loadAll(long groupAddr, long addr, int nodeId, float scale, float x, float y, float z, WorldLoadListener listener,
            List<CompletableFuture<Void>> futures, boolean noGenerate) {
        // Sanity checks (for debugging)
        assert groupAddr != 0;
        assert addr != 0;
        assert scale != 0;
        assert listener != null;
        assert futures != null;
        
        System.out.println("Read flags from: " + addr);
        byte flags = mem.readVolatileByte(addr);
        System.out.println(Integer.toBinaryString(flags));
        float childScale = scale * 0.5f;
        
        System.out.println("scale: " + scale);
        // TODO check if I need to modify this to be a tail call manually (does the stack overflow?)
        float posMod = 0.25f * scale;
        if (childScale == DataConstants.CHUNK_SCALE) { // Nodes might be chunks
            for (int i = 0; i < 8; i++) {
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
                
                if ((flags >>> i & 1) == 1) { // Chunk, we need to make sure it is loaded
                    long nodeAddr = addr + 1 + i * DataConstants.OCTREE_NODE_SIZE;
                    int node = mem.readVolatileInt(nodeAddr);
                    System.err.println("nodeAddr: " + nodeAddr + ", node: " + (node & 0xffff) + ", buf: " + (node >>> 16));
                    if (node == 0 && !noGenerate) {
                        float fX = x2;
                        float fY = y2;
                        float fZ = z2;
                        //CompletableFuture<Void> future = CompletableFuture.runAsync(() -> { // Schedule world gen to be done async
                            int newNode = handleGenerate(fX, fY, fZ);
                            mem.compareAndSwapInt(nodeAddr, 0, newNode);
                            System.err.println("gen node: " + (newNode & 0xffff) + ", buf: " + (newNode >>> 16));
                            // TODO clear garbage produced by race conditions somehow
                            listener.chunkLoaded(chunkStorage.getTemporaryChunk(node, registry), fX, fY, fZ);
                        //});
                        // Put joinable future to list of them, if caller wants to make sure they're all done
                        //futures.add(future);
                    } else {
                        listener.chunkLoaded(chunkStorage.getTemporaryChunk(node, registry), x2, y2, z2);
                    }
                }
                
                // Single octree nodes are loaded already
            }
        } else { // Nodes might be octrees
            for (int i = 0; i < 8; i++) {
                if ((flags >>> i & 1) == 1) { // Octree, we need to make sure it is loaded
                    long nodeAddr = addr + 1 + i * DataConstants.OCTREE_NODE_SIZE;
                    
                    // Check if there is an octree; if not, create one
                    if (mem.readVolatileInt(nodeAddr) == 0) {
                        if (noGenerate) { // If generation is disallowed, ignore this
                            continue;
                        } else { // Else create octree
                            int octreeIndex = octreeStorage.newOctree(); // Allocate new octree
                            mem.compareAndSwapInt(nodeAddr, 0, octreeIndex); // if no one else allocated it yet, save index
                            // This creates empty octrees, but probably not often
                        }
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
                    
                    int data = mem.readVolatileInt(nodeAddr);
                    int newGroup = data >>> 24;
                    int newIndex = data & 0xffffff;
                    long newGroupAddr = octreeStorage.getGroup(newGroup);
                    loadAll(newGroupAddr, newGroupAddr + newIndex * DataConstants.OCTREE_SIZE, data, childScale, x2, y2, z2, listener, futures, noGenerate);
                } else {
                    System.out.println("Single node, scale: " + scale);
                }
            }
            
            // Fire octree loaded once it is completely (or if noGenerate was passed, somewhat) generated
            listener.octreeLoaded(addr, groupAddr, nodeId, x, y, z, scale);
        }
    }
    
    public int handleGenerate(float x, float y, float z) {
        System.out.println("Handle generation...");
        
        // Prepare data which generator will fill
        short[] data = new short[DataConstants.CHUNK_MAX_BLOCKS];
        WorldGenerator.Metadata meta = new WorldGenerator.Metadata();
        
        generator.generate(data, x, y, z, DataConstants.CHUNK_SCALE, meta);
        
        int chunkId = chunkStorage.newChunk(); // Create a chunk
        ChunkBuffer buf = chunkStorage.getBuffer(chunkId >>> 16); // Use buffer id from chunk it to get buffer
        int index = chunkId & 0xffff; // Get index inside buffer
        
        // TODO use data heuristics to get the type
        buf.setChunkType(index, ChunkType.UNCOMPRESSED);
        
        long chunkAddr = mem.allocate(DataConstants.CHUNK_UNCOMPRESSED);
        for (int i = 0; i < data.length; i++) {
            mem.writeShort(chunkAddr + i * 2, data[i]);
        }
        buf.setChunkAddr(index, chunkAddr);
        buf.setChunkLength(index, DataConstants.CHUNK_UNCOMPRESSED);
        buf.setChunkUsed(index, DataConstants.CHUNK_UNCOMPRESSED);
        
        return chunkId;
    }
    
    public OctreeStorage getOctreeStorage() {
        return octreeStorage;
    }
    
    public ChunkStorage getChunkStorage() {
        return chunkStorage;
    }

    @Override
    public void addLoadMarker(LoadMarker marker) {
        loadMarkers.add(marker);
        loadMarkers.sort(Comparator.reverseOrder()); // Sort most important first
    }
    

    @Override
    public void removeLoadMarker(LoadMarker marker) {
        Iterator<LoadMarker> it = loadMarkers.iterator();
        while (it.hasNext()) {
            LoadMarker m = it.next();
            if (m == marker) {
                it.remove();
                return;
            }
        }
    }

    @Override
    public List<CompletableFuture<Void>> updateLoadMarkers() {
        List<CompletableFuture<Void>> pendingMarkers = new ArrayList<>(loadMarkers.size());
        // Delegate updating to async code, this might be costly
        for (LoadMarker marker : loadMarkers) {
            if (marker.hasMoved()) { // Update only marker that has been moved
                // When player moves a little, DO NOT, I repeat, DO NOT just blindly move load marker.
                // Move it when player has moved a few meters or so!
                pendingMarkers.add(CompletableFuture.runAsync(() -> updateLoadMarker(marker, loadListener, false), storageExecutor));
            }
        }
        
        return pendingMarkers;
    }
    
    public List<CompletableFuture<Void>> updateLoadMarkers(WorldLoadListener listener, boolean soft, boolean ignoreMoved) {
        List<CompletableFuture<Void>> pendingMarkers = new ArrayList<>(loadMarkers.size());
        // Delegate updating to async code, this might be costly
        for (LoadMarker marker : loadMarkers) {
            if (ignoreMoved || marker.hasMoved()) { // Update only marker that has been moved
                // When player moves a little, DO NOT, I repeat, DO NOT just blindly move load marker.
                // Move it when player has moved a few meters or so!
                pendingMarkers.add(CompletableFuture.runAsync(() -> updateLoadMarker(marker, listener, soft), storageExecutor));
            }
        }
        
        return pendingMarkers;
    }
    
    /**
     * Updates given load marker no matter what. Only used internally.
     * @param marker Load marker to update.
     * @param listener Load listener.
     * @param soft If soft radius should be used.
     */
    private void updateLoadMarker(LoadMarker marker, WorldLoadListener listener, boolean soft) {
        System.out.println("Update load marker...");
        loadArea(marker.getX(), marker.getY(), marker.getZ(), soft ? marker.getSoftRadius() : marker.getHardRadius(), listener, soft);
        marker.markUpdated(); // Tell it we updated it
    }
    
    public void setLoadListener(WorldLoadListener listener) {
        this.loadListener = listener;
    }
    
    public void requestUnload() {
        memManager.queueUnload();
    }
    
    public void updateMasterOctree() {
        System.out.println("masterIndex: " + octreeStorage.getMasterIndex());
        masterOctree = octreeStorage.getOctree(octreeStorage.getMasterIndex(), registry);
        masterScale = octreeStorage.getMasterScale(128); // TODO need to have this CONFIGURABLE!
        centerX = octreeStorage.getCenterPoint(0);
        centerY = octreeStorage.getCenterPoint(1);
        centerZ = octreeStorage.getCenterPoint(2);
        System.out.println("world center: " + centerX + ", " + centerY + ", " + centerZ + ", scale: " + masterScale);
        mem.writeByte(masterOctree.memoryAddress(), (byte) 0xff); // Just in case, master octree has no single nodes
    }

    public float getMasterScale() {
        return masterScale;
    }

}
