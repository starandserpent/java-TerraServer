package com.ritualsoftheold.terra.offheap.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.IntToLongFunction;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.node.Chunk;
import com.ritualsoftheold.terra.node.Node;
import com.ritualsoftheold.terra.node.Octree;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.Pointer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkStorage;
import com.ritualsoftheold.terra.offheap.data.TypeSelector;
import com.ritualsoftheold.terra.offheap.io.ChunkLoader;
import com.ritualsoftheold.terra.offheap.io.OctreeLoader;
import com.ritualsoftheold.terra.offheap.memory.MemoryManager;
import com.ritualsoftheold.terra.offheap.memory.MemoryPanicHandler;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;
import com.ritualsoftheold.terra.offheap.node.OffheapOctree;
import com.ritualsoftheold.terra.offheap.octree.OctreeStorage;
import com.ritualsoftheold.terra.offheap.octree.UsageListener;
import com.ritualsoftheold.terra.offheap.verifier.TerraVerifier;
import com.ritualsoftheold.terra.offheap.world.gen.WorldGenManager;
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
    private WorldGenerator<?> generator;
    private WorldGenManager genManager;
    private Executor generatorExecutor;
    
    private MaterialRegistry registry;
    
    // Load markers
    private List<OffheapLoadMarker> loadMarkers;
    private WorldLoadListener loadListener;
    
    // Memory management
    private MemoryManager memManager;
    
    private WorldSizeManager sizeManager;
    
    // Coordinates of world center
    private float centerX;
    private float centerY;
    private float centerZ;
    
    // New world loader, no more huge methods in this class!
    private WorldLoader worldLoader;
    
    private UsageListener octreeUsageListener;
    
    public static class Builder {
        
        private OffheapWorld world;
        
        private int octreeGroupSize;
        
        private long memPreferred;
        private long memMax;
        private MemoryPanicHandler memPanicHandler;
        
        private ChunkBuffer.Builder chunkBufferBuilder;
        private int chunkMaxBuffers;
        
        private boolean perNodeReady;
        
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
        
        public Builder generator(WorldGenerator<?> generator) {
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
        
        public Builder perNodeReadyCheck(boolean enabled) {
            this.perNodeReady = enabled;
            return this;
        }
        
        public Builder octreeUsageListener(UsageListener listener) {
            world.octreeUsageListener = listener;
            return this;
        }
        
        public OffheapWorld build() {
            // Initialize some internal structures AFTER all user-controller initialization
            world.loadMarkers = new ArrayList<>();
            world.sizeManager = new WorldSizeManager(world);
            
            // Create memory manager
            world.memManager = new MemoryManager(world, memPreferred, memMax, memPanicHandler);
            
            // Initialize stuff that needs memory manager
            world.octreeStorage = new OctreeStorage(octreeGroupSize, world.octreeLoader, world.storageExecutor, world.memManager,
                    perNodeReady, world.octreeUsageListener);
            chunkBufferBuilder.memListener(world.memManager);
            chunkBufferBuilder.perChunkReady(perNodeReady);
            world.chunkStorage = new ChunkStorage(world.registry, chunkBufferBuilder, chunkMaxBuffers, world.chunkLoader, world.storageExecutor);
            
            // Initialize memory manager with storages
            world.memManager.initialize(world.octreeStorage, world.chunkStorage);
            
            // Initialize world generation
            world.genManager = new WorldGenManager(world.generator, new TypeSelector(), world);
            
            // ... and world loading
            world.worldLoader = new WorldLoader(world.octreeStorage, world.chunkStorage, world.genManager, new WorldSizeManager(world));
            
            // Update master octree (and finish loader stuff)
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
            return chunkStorage.getChunk(nodeId);
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
        return 0; // TODO redo this
    }
    
    public OctreeStorage getOctreeStorage() {
        return octreeStorage;
    }
    
    public ChunkStorage getChunkStorage() {
        return chunkStorage;
    }
    
    private OffheapLoadMarker checkLoadMarker(LoadMarker marker) {
        if (!(marker instanceof OffheapLoadMarker))
            throw new IllegalArgumentException("incompatible load marker");
        return (OffheapLoadMarker) marker;
    }

    @Override
    public void addLoadMarker(LoadMarker marker) {
        loadMarkers.add(checkLoadMarker(marker));
        loadMarkers.sort(Comparator.reverseOrder()); // Sort most important first
    }
    

    @Override
    public void removeLoadMarker(LoadMarker marker) {
        checkLoadMarker(marker); // Wrong type wouldn't actually break anything
        // But user probably wants to know if they're passing wrong marker to us
        
        Iterator<OffheapLoadMarker> it = loadMarkers.iterator();
        while (it.hasNext()) {
            OffheapLoadMarker m = it.next();
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
        for (OffheapLoadMarker marker : loadMarkers) {
            if (marker.hasMoved()) { // Update only marker that has been moved
                // When player moves a little, DO NOT, I repeat, DO NOT just blindly move load marker.
                // Move it when player has moved a few meters or so!
                pendingMarkers.add(CompletableFuture.runAsync(() -> updateLoadMarker(marker, loadListener, false), storageExecutor)
                        .exceptionally((e) -> {
                            e.printStackTrace(); // TODO better error handling
                            return null;
                        }));
            }
        }
        
        return pendingMarkers;
    }
    
    public List<CompletableFuture<Void>> updateLoadMarkers(WorldLoadListener listener, boolean soft, boolean ignoreMoved) {
        List<CompletableFuture<Void>> pendingMarkers = new ArrayList<>(loadMarkers.size());
        // Delegate updating to async code, this might be costly
        for (OffheapLoadMarker marker : loadMarkers) {
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
    private void updateLoadMarker(OffheapLoadMarker marker, WorldLoadListener listener, boolean soft) {
        OffheapLoadMarker oldMarkerClone = marker.clone(); // Keep copy of old load marks for a while
        marker.clear(); // Remove all of them from original marker
        
        // Tell world loader to load stuff, and while doing so, update the load marker
        worldLoader.seekArea(marker.getX(), marker.getY(), marker.getZ(),
                soft ? marker.getSoftRadius() : marker.getHardRadius(), listener, !soft, marker);
        marker.markUpdated(); // Tell it we updated it
        
        // Allow unloading things that previous marker kept loaded
        // (unless new marker also requires them, of course)
        chunkStorage.removeLoadMarker(oldMarkerClone);
        octreeStorage.removeLoadMarker(oldMarkerClone);
    }
    
    /**
     * Sets default load listener for this world. It will be used when loading
     * world with TerraWorld's API; this implementation also allows you to
     * override the load listener.
     * @param listener Load listener.
     */
    public void setLoadListener(WorldLoadListener listener) {
        this.loadListener = listener;
    }
    
    /**
     * Requests memory manager to start unloading data if that is needed.
     */
    public void requestUnload() {
        memManager.queueUnload();
    }
    
    /**
     * Updates master octree data from offheap data storage.
     */
    public void updateMasterOctree() {
        System.out.println("masterIndex: " + octreeStorage.getMasterIndex());
        int masterIndex = octreeStorage.getMasterIndex();
        // TODO cleanup
//        masterOctree = octreeStorage.getOctree(masterIndex, registry); // TODO do we really need OffheapOctree in world for this?
        masterScale = octreeStorage.getMasterScale(2048); // TODO need to have this CONFIGURABLE!
        centerX = octreeStorage.getCenterPoint(0);
        centerY = octreeStorage.getCenterPoint(1);
        centerZ = octreeStorage.getCenterPoint(2);
        System.out.println("world center: " + centerX + ", " + centerY + ", " + centerZ + ", scale: " + masterScale);
        mem.writeByte(octreeStorage.getOctreeAddrInternal(masterIndex), (byte) 0xff); // Just in case, master octree has no single nodes
        
        // Update relevant data to world loader
        worldLoader.worldConfig(centerX, centerY, centerZ, masterIndex, masterScale);
    }

    /**
     * Gets current scale of master octree of this world.
     * @return Master octree scale.
     */
    public float getMasterScale() {
        return masterScale;
    }
    
    /**
     * Resulting data from chunk (potentially failed) copy operation.
     *
     */
    public static class CopyChunkResult {
        
        private byte type;
        private int length;
        private boolean success;
        
        public CopyChunkResult(boolean success, byte type, int length) {
            this.success = success;
            this.type = type;
            this.length = length;
        }
        
        /**
         * Checks if the copy operation was successful.
         * @return If it succeeded.
         */
        public boolean isSuccess() {
            return success;
        }
        
        /**
         * Gets the type of chunk.
         * @return Type of chunk.
         */
        public byte getType() {
            return type;
        }
        
        /**
         * Gets length of chunk data.
         * @return Length of chunk data.
         */
        public int getLength() {
            return length;
        }
    }
    
    /**
     * Copies chunk data for given chunk to given memory address.
     * Make sure there is enough space allocated!
     * @param id Chunk id.
     * @param target Target memory address
     * @return Relevant information about copied chunk.
     */
    public CopyChunkResult copyChunkData(int id, @Pointer long target) {
        int bufId = id >>> 16;
        chunkStorage.markUsed(bufId);
        
        // Get buffer and relevant memory addresses
        ChunkBuffer buf = chunkStorage.getOrLoadBuffer(bufId);
        int index = id & 0xffff;
        int length;
        byte type;
        try (OffheapChunk.Storage storage = buf.getChunk(index).getStorage()) {
            long addr = storage.address;
            length = storage.length;
            type = storage.format.getChunkType();
            
            // Only copy if there is something to copy
            if (length > 0) {
                mem.copyMemory(addr, target, length);
            }
        }
        
        chunkStorage.markUnused(bufId);
        
        return new CopyChunkResult(true, type, length);
    }
    
    /**
     * Attempts to copy data of given chunk. You'll provide a function which
     * can give memory addresses based on length of data.
     * @param id Chunk id.
     * @param handler Function to provide target memory address. It will
     * receive chunk data length as an argument. If it returns 0, copy
     * operation will fail (but no exception is thrown).
     * @return Relevant information about copied chunk, or indication of
     * failure to copy it.
     */
    public CopyChunkResult copyChunkData(int id, IntToLongFunction handler) {
        int bufId = id >>> 16;
        chunkStorage.markUsed(bufId);
        
        // Get buffer and relevant memory addresses
        ChunkBuffer buf = chunkStorage.getOrLoadBuffer(bufId);
        int index = id & 0xffff;
        int length;
        byte type;
        try (OffheapChunk.Storage storage = buf.getChunk(index).getStorage()) {
            long addr = storage.address;
            length = storage.length;
            type = storage.format.getChunkType();
            
            // Only copy if there is something to copy
            if (length > 0) {
                long target = handler.applyAsLong(length);
                if (target == 0) { // Chunk length is more than 0 but we got NULL pointer...
                    return new CopyChunkResult(false, (byte) 0, 0); // Fail
                }
                mem.copyMemory(addr, target, length);
            }
        }
        
        chunkStorage.markUnused(bufId);
        
        return new CopyChunkResult(true, type, length);
    }
    
    /**
     * Copies an octree group or parts of one to given memory address.
     * Make sure there is enough space allocated!
     * @param index Group index.
     * @param target Target memory address.
     * @param beginIndex Index of first octree to be copied.
     * @param endIndex Index of last octree to be copied. Negative values
     * are interpreted as last octree of the group
     * @return How much bytes were eventually copied.
     */
    public int copyOctreeGroup(int index, @Pointer long target, int beginIndex, int endIndex) {
        if (endIndex < 0) { // Default to last octree in group
            endIndex = octreeStorage.getGroupSize() - 1;
        }
        
        // Calculate count, validate it, if passed then calculate length
        int count = endIndex - beginIndex;
        if (beginIndex < 0) {
            throw new IllegalArgumentException("beginIndex cannot be less than 0");
        }
        if (endIndex >= octreeStorage.getGroupSize()) {
            throw new IllegalArgumentException("endIndex cannot be greater than size of group");
        }
        if (count < 1) {
            throw new IllegalArgumentException("copying less than one octree is not possible");
        }
        int length = count * DataConstants.OCTREE_SIZE;
        
        octreeStorage.markUsed(index); // Prevent group from being unloaded
        
        // Get address of group and copy relevant nodes
        long addr = octreeStorage.getGroup(index);
        mem.copyMemory(addr + beginIndex * DataConstants.OCTREE_SIZE, target + endIndex * DataConstants.OCTREE_SIZE, length);
        
        octreeStorage.markUnused(index); // Allow unloading again, copy finished
        
        return length;
    }
    
    /**
     * Creates a new data verifier, configured to work with settings of this
     * world. It is not valid for any other worlds!
     * @return A new Terra verifier for this world.
     */
    public TerraVerifier createVerifier() {
        return new TerraVerifier(octreeStorage.getGroupSize(), chunkStorage.getBufferBuilder().maxChunks(),
                chunkStorage.getAllBuffers().length());
    }

    @Override
    public LoadMarker createLoadMarker(float x, float y, float z, float hardRadius, float softRadius, int priority) {
        return new OffheapLoadMarker(x, y, z, hardRadius, softRadius, priority, octreeUsageListener);
    }
}
