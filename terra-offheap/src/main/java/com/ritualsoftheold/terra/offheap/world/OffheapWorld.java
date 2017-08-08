package com.ritualsoftheold.terra.offheap.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.StampedLock;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.node.Chunk;
import com.ritualsoftheold.terra.node.Node;
import com.ritualsoftheold.terra.node.Octree;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.ChunkStorage;
import com.ritualsoftheold.terra.offheap.io.ChunkLoader;
import com.ritualsoftheold.terra.offheap.io.OctreeLoader;
import com.ritualsoftheold.terra.offheap.memory.MemoryManager;
import com.ritualsoftheold.terra.offheap.memory.MemoryPanicHandler;
import com.ritualsoftheold.terra.offheap.memory.MemoryUseListener;
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
    
    /**
     * A lock for enter() and leave().
     */
    private StampedLock lock;
    private StampedLock exclusivePending;
    
    public OffheapWorld(ChunkLoader chunkLoader, OctreeLoader octreeLoader, MaterialRegistry registry, WorldGenerator generator) {
        this.chunkLoader = chunkLoader;
        this.octreeLoader = octreeLoader;
        
        // Initialize storages
        this.storageExecutor = new ForkJoinPool();
        this.chunkStorage = new ChunkStorage(chunkLoader, storageExecutor, 64, 1024); // TODO settings for sizes of storages
        this.octreeStorage = new OctreeStorage(8192 * DataConstants.OCTREE_SIZE, octreeLoader, storageExecutor);
        
        // Initialize master octree only after memory manager is present
        
        this.loadMarkers = new ArrayList<>();
        
        this.registry = registry;
        
        this.generatorExecutor = new ForkJoinPool();
        this.generator = generator;
        
        // Initialize access locks
        this.lock = new StampedLock();
        this.exclusivePending = new StampedLock();
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
            return octreeStorage.getOctree(nodeId, this);
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
        long addr = masterOctree.memoryAddress(); // Get starting memory address
        System.out.println("addr: " + addr);
        listener.octreeLoaded(addr, octreeStorage.getGroup((byte) 0), x, y, z, masterScale);
        
        float scale = masterScale; // Starting scale
        int entry = 0; // Chunk or octree id
        boolean isOctree = true; // If the final entry is chunk or octree
        
        // Store original values. We're going to need them
        float originX = x;
        float originY = y;
        float originZ = z;
        
        long groupAddr = 0;
        
        float octreeX = 0, octreeY = 0, octreeZ = 0;
        while (true) {
            float farthestPoint = 0.5f * scale; // Farthest point from center 
            if (originX + radius > x + farthestPoint || originX - radius < x - farthestPoint
                    || originY + radius > y + farthestPoint || originY - radius < y - farthestPoint
                    || originZ + radius > z + farthestPoint || originZ - radius < z - farthestPoint) {
                // We found small enough unit... load everything inside it!
                System.out.println("Radius limit hit...");
                System.out.println("coords: " + x + ", " + y + ", " + z);
                System.out.println("scale: " + scale);
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
            
            // I hope volatile is enough to avoid race conditions
            System.out.println("Read flags: " + addr);
            isOctree = (mem.readVolatileByte(addr) >>> index & 1) == 1; // Get flags, check this index against them
            scale *= 0.5f; // Halve the scale, we are moving to child node
            
            long nodeAddr = addr + 1 + index * DataConstants.OCTREE_NODE_SIZE; // Get address of the node
            
            if (scale < DataConstants.CHUNK_SCALE + 1) { // Found a chunk
                // Check if there is a chunk; if not, generate one
                if (mem.readVolatileInt(nodeAddr) == 0) {
                    // TODO
                    System.out.println("TODO here!");
                }
                
                break;
            } else { // Just octree or single block here
                if (isOctree) {
                    // Check if there is an octree; if not, create one
                    System.out.println("Octree: " + mem.readVolatileInt(nodeAddr));
                    if (mem.readVolatileInt(nodeAddr) == 0 && !noGenerate) {
                        int octreeIndex = octreeStorage.newOctree(); // Allocate new octree
                        System.out.println("CAS octree: " + (octreeIndex >>> 24));
                        boolean success = mem.compareAndSwapInt(nodeAddr, 0, octreeIndex); // if no one else allocated it yet, save index
                        System.out.println("CAS result: " + success);
                        // This creates empty octrees, but probably not often
                    }
                    groupAddr = octreeStorage.getGroup(mem.readVolatileByte(nodeAddr)); // First byte of octree node is group addr!
                    System.out.println("groupIndex: " + mem.readVolatileByte(nodeAddr) + ", groupAddr: " + groupAddr);
                    
                    addr = groupAddr + (mem.readVolatileInt(nodeAddr) >>> 8) * DataConstants.OCTREE_SIZE; // Update address to point to new octree
                    
                    listener.octreeLoaded(addr, groupAddr, x, y, z, scale);
                } else {
                    System.out.println("Single node: " + scale);
                    break;
                }
            }
        }
        
        // If we needed to only load one chunk, that is done already
        if (!isOctree) {
            return;
        }
        
        // Prepare to loadAll, then join all remaining futures here
        List<CompletableFuture<Void>> futures = new ArrayList<>((int) (scale / 16 * scale / 16 * scale / 16) + 10); // Assume size of coming futures
        loadAll(groupAddr, addr, scale, x, y, z, listener, futures, noGenerate);
        for (CompletableFuture<Void> future : futures) { // Join all futures now
            future.join();
        }
    }
    
    /**
     * Starts loading all children of given octree index.
     */
    private void loadAll(long groupAddr, long addr, float scale, float x, float y, float z, WorldLoadListener listener,
            List<CompletableFuture<Void>> futures, boolean noGenerate) {
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
                    if (node == 0 && !noGenerate) {
                        float fX = x2;
                        float fY = y2;
                        float fZ = z2;
                        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> { // Schedule world gen to be done async
                            int newNode = handleGenerate(fX, fY, fZ);
                            mem.compareAndSwapInt(nodeAddr, 0, newNode);
                            // TODO clear garbage produced by race conditions somehow
                            listener.chunkLoaded(chunkStorage.ensureLoaded(newNode), chunkStorage.getBufferUnsafe((short) (node >>> 16)), fX, fY, fZ);
                        });
                        // Put joinable future to list of them, if caller wants to make sure they're all done
                        futures.add(future);
                    } else {
                        listener.chunkLoaded(chunkStorage.ensureLoaded(node), chunkStorage.getBufferUnsafe((short) (node >>> 16)), x2, y2, z2);
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
                    
                    long newGroupAddr = octreeStorage.getGroup(mem.readVolatileByte(nodeAddr));
                    loadAll(newGroupAddr, groupAddr + (mem.readVolatileInt(nodeAddr) >>> 8) * DataConstants.OCTREE_SIZE, childScale, x2, y2, z2, listener, futures, noGenerate);
                } else {
                    System.out.println("Single node, scale: " + scale);
                }
            }
            
            // Fire octree loaded once it is completely (or if noGenerate was passed, somewhat) generated
            listener.octreeLoaded(addr, groupAddr, x, y, z, scale);
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
        System.out.println("Handle generation...");
        short[] data = new short[DataConstants.CHUNK_MAX_BLOCKS];
        generator.generate(data, x, y, z, DataConstants.CHUNK_SCALE);
        
        long tempAddr = mem.allocate(DataConstants.CHUNK_UNCOMPRESSED);
        
        // TODO memory copy is faster (but not as safe)
        for (int i = 0; i < data.length; i++) {
            mem.writeShort(tempAddr + i * 2, data[i]);
        }
        
        int chunkId = chunkStorage.addChunk(tempAddr, registry);
        mem.freeMemory(tempAddr, DataConstants.CHUNK_UNCOMPRESSED);
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
    
    /**
     * Waits for pending exclusive locks.
     */
    private void waitForExclusive() {
        long stamp = exclusivePending.readLock();
        exclusivePending.unlockRead(stamp);
    }

    @Override
    public long enter() {
        waitForExclusive();
        return enterNow();
    }

    @Override
    public long enterNow() {
        return lock.readLock(); // Just get read lock
    }

    @Override
    public void leave(long stamp) {
        lock.unlockRead(stamp); // Just release read access
    }
    
    public long enterExclusive() {
        // Request that no further enter() will get accepted until we're done; enterNow() will work
        long pendingStamp = exclusivePending.writeLock();
        long stamp = lock.writeLock(); // Try acquire write lock; this will succeed once readers have left
        exclusivePending.unlockWrite(pendingStamp); // We have exclusive access, unlock pending lock
        // All threads which were blocking on exclusivePending will block on lock now
        
        return stamp; // Just return the stamp. User better be careful with it!
    }
    
    public void leaveExclusive(long stamp) {
        lock.unlockWrite(stamp);
    }
    
    public void requestUnload() {
        memManager.queueUnload();
    }
    
    public void setMemorySettings(long preferred, long max, MemoryPanicHandler panicHandler) {
        this.memManager = new MemoryManager(this, preferred, max, panicHandler);
        octreeStorage.setMemListener(memManager);
        chunkStorage.setMemListener(memManager);
        
        updateMasterOctree();
        System.out.println("Write flags to " + masterOctree.memoryAddress());
    }
    
    public void updateMasterOctree() {
        masterOctree = octreeStorage.getOctree(octreeStorage.getMasterIndex(), this);
        masterScale = octreeStorage.getMasterScale(32); // TODO need to have this CONFIGURABLE!
        mem.writeByte(masterOctree.memoryAddress(), (byte) 0xff); // Just in case, master octree has no single nodes
    }

}
