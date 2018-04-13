package com.ritualsoftheold.terra.offheap.world.gen;

import com.ritualsoftheold.terra.buffer.BlockBuffer;
import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.offheap.chunk.ChunkStorage;
import com.ritualsoftheold.terra.offheap.chunk.WrappedCriticalBuffer;
import com.ritualsoftheold.terra.offheap.chunk.compress.ChunkFormat;
import com.ritualsoftheold.terra.offheap.data.MemoryAllocator;
import com.ritualsoftheold.terra.offheap.data.TypeSelector;
import com.ritualsoftheold.terra.offheap.data.WorldDataFormat;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk.Storage;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;
import com.ritualsoftheold.terra.world.gen.GenerationTask;
import com.ritualsoftheold.terra.world.gen.Pipeline;
import com.ritualsoftheold.terra.world.gen.WorldGenerator;

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
    private WorldGenerator<Object> generator;
    
    /**
     * Some "clever" code to determine what data format to use.
     */
    private TypeSelector typeSelector;
    
    private ChunkStorage chunkStorage;
    
    private MaterialRegistry materialRegistry;
    
    private OffheapWorld world;
    
    public WorldGenManager(WorldGenerator<Object> generator, TypeSelector typeSelector, ChunkStorage chunkStorage) {
        this.generator = generator;
        this.typeSelector = typeSelector;
        this.chunkStorage = chunkStorage;
    }
    
    /**
     * Generates a piece of world at given coordinates with given scale.
     * The parent octree will be modified to include reference to
     * data once generation is complete.
     * @param address Address of parent octree.
     * @param index Index of the piece in its parent octree.
     * @param x X coordinate of center.
     * @param y Y coordinate of center.
     * @param z Z coordinate of center.
     * @param scale Scale.
     */
    public void generate(long addr, int index, float x, float y, float z, float scale) {
        OffheapGeneratorControl control = new OffheapGeneratorControl(new SelfTrackAllocator());
        GenerationTask task = new GenerationTask(x, y, z);
        OffheapPipeline<Object> pipeline = new OffheapPipeline<>();
        
        // Ask world generator to initialize task
        Object meta = generator.initialize(task, (Pipeline<Object>) pipeline);
        
        // Execute the whole generation pipeline!
        pipeline.execute(task, control, (Object) meta);
    }

    public BlockBuffer createBuffer(int size, MemoryAllocator allocator) {
        WorldDataFormat format = typeSelector.getDataFormat(size);
        
        BlockBuffer wrapped; // Buffer we will wrap
        Storage storage;
        if (format instanceof ChunkFormat) {
            int firstLen = ((ChunkFormat) format).newDataLength();
            long addr = allocator.alloc(firstLen);
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
