package com.ritualsoftheold.terra.offheap.world.gen;

import com.ritualsoftheold.terra.core.buffer.BlockBuffer;
import com.ritualsoftheold.terra.core.gen.tasks.GenerationTask;
import com.ritualsoftheold.terra.core.gen.tasks.Pipeline;
import com.ritualsoftheold.terra.core.material.MaterialRegistry;
import com.ritualsoftheold.terra.offheap.memory.MemoryAllocator;
import com.ritualsoftheold.terra.offheap.memory.MemoryUseListener;
import com.ritualsoftheold.terra.offheap.octree.OctreeNodeFormat;
import com.ritualsoftheold.terra.offheap.memory.SelfTrackAllocator;
import com.ritualsoftheold.terra.core.gen.interfaces.world.WorldGeneratorInterface;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkStorage;
import com.ritualsoftheold.terra.offheap.chunk.WrappedCriticalBuffer;
import com.ritualsoftheold.terra.offheap.chunk.compress.ChunkFormat;
import com.ritualsoftheold.terra.offheap.data.CriticalBlockBuffer;
import com.ritualsoftheold.terra.offheap.data.TypeSelector;
import com.ritualsoftheold.terra.offheap.data.WorldDataFormat;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk.Storage;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Manages generating the world using an underlying world generator.
 * This way world generators don't have to deal with Terra's internals.
 *
 */
public class WorldGenManager {
    
    private static final Memory mem = OS.memory();
    
    /**
     * World generator which does the actual hard work:
     * fills arrays after each other with blocks.
     */
    private WorldGeneratorInterface<Object> generator;
    
    /**
     * Some "clever" code to determine what data format to use.
     */
    private TypeSelector typeSelector;
    
    private ChunkStorage chunkStorage;

    private MaterialRegistry materialRegistry;
    
    private OffheapWorld world;
    
    @SuppressWarnings("unchecked") // I hate generics
    public WorldGenManager(WorldGeneratorInterface<?> generator, TypeSelector typeSelector, OffheapWorld world) {
        this.generator = (WorldGeneratorInterface<Object>) generator;
        this.typeSelector = typeSelector;
        this.chunkStorage = world.getChunkStorage();
        this.world = world;
    }
    
    /**
     * Generates a piece of world at given coordinates with given scale.
     * The parent octree will be modified to include reference to
     * data once generation is complete.
     * @param addr Address of parent octree.
     * @param index Index of the piece in its parent octree.
     * @param x X coordinate of center.
     * @param y Y coordinate of center.
     * @param z Z coordinate of center.
     * @param scale Scale.
     */
    public void generate(long addr, int index, float x, float y, float z, float scale) {
        SelfTrackAllocator trackedAllocator = new SelfTrackAllocator(true); // Must zero that memory!
        OffheapGeneratorControl control = new OffheapGeneratorControl(this, trackedAllocator);
        GenerationTask task = new GenerationTask(x, y, z);
        OffheapPipeline<Object> pipeline = new OffheapPipeline<>();
        
        // Ask world generator to initialize task
        Object meta = generator.initialize(task, pipeline);
        
        // Execute the whole generation pipeline!
        pipeline.execute(task, control, meta);
        
        // Take results of the execution
        CriticalBlockBuffer buf = control.getBuffer();
        if (buf == null) { // Nothing was generated
            // TODO handle it
            System.out.println("NULL");
        } else {
            WorldDataFormat format = buf.getDataFormat();
            if (format.isOctree()) {
                OctreeNodeFormat octreeFormat = (OctreeNodeFormat) format;
                octreeFormat.setNode(addr, index, buf.read().getWorldId()); // World id to octree
                octreeFormat.modifyFlag(addr, index, 0); // Single node
            } else {
                // Acquire a new chunk
                int chunkId = chunkStorage.newChunk();
                if (!mem.compareAndSwapInt(addr + index * 4, 0, chunkId)) {
                    // Someone got there before us!
                    // TODO deal with trash
                    return; // -> they get to do this
                }
                // Flag for this is 1, because we are generating for first time
                // (all flags are 1 at this point)
                
                // Notify memory use listener about memory we used
                MemoryUseListener memListener = chunkStorage.getBufferBuilder().memListener();
                memListener.onAllocate(trackedAllocator.getMemoryUsed());
                
                ChunkBuffer chunkBuf = chunkStorage.getBuffer(chunkId >>> 16);
                OffheapChunk chunk = chunkBuf.getChunk(chunkId & 0xffff);
                
                // Set its storage to generated contents
                Storage storage = buf.getStorage();
                chunk.setStorageInternal(storage);
            }
        }
    }

    public CriticalBlockBuffer createBuffer(int size, MemoryAllocator allocator) {
        WorldDataFormat format = typeSelector.getDataFormat(size);

        BlockBuffer wrapped; // Buffer we will wrap
        Storage storage;
        if (format instanceof ChunkFormat) {
            int firstLen = ((ChunkFormat) format).newDataLength();
            long addr = allocator.allocate(firstLen);
            storage = new Storage((ChunkFormat) format, addr, firstLen);
            wrapped = ((ChunkFormat) format).createCriticalBuffer(storage, materialRegistry);
        } else {
            // TODO octree block buffers
            wrapped = null;
            storage = null;
        }

        // Now, wrap the buffer
        return new WrappedCriticalBuffer((ChunkFormat) format, wrapped, storage, allocator, typeSelector, world.getMaterialRegistry());
    }
}
