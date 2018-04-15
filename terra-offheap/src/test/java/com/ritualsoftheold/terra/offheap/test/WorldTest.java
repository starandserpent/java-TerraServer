package com.ritualsoftheold.terra.offheap.test;

import static org.junit.Assert.*;

import java.util.concurrent.ForkJoinPool;

import org.junit.Before;
import org.junit.Test;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyChunkLoader;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyOctreeLoader;
import com.ritualsoftheold.terra.offheap.memory.MemoryPanicHandler;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;
import com.ritualsoftheold.terra.offheap.world.WorldLoadListener;
import com.ritualsoftheold.terra.world.LoadMarker;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Tests OffheapWorld.
 *
 */
public class WorldTest {
    
    private static final Memory mem = OS.memory();
    
    private OffheapWorld world;

    @Before
    public void init() {
        ChunkBuffer.Builder bufferBuilder = new ChunkBuffer.Builder()
                .maxChunks(128)
                .queueSize(4);
        
        world = new OffheapWorld.Builder()
                .chunkLoader(new DummyChunkLoader())
                .octreeLoader(new DummyOctreeLoader(32768))
                .storageExecutor(ForkJoinPool.commonPool())
                .chunkStorage(bufferBuilder, 128)
                .octreeStorage(32768)
                .generator(new TestWorldGenerator())
                .generatorExecutor(ForkJoinPool.commonPool())
                .materialRegistry(new MaterialRegistry())
                .memorySettings(10000000, 10000000, new MemoryPanicHandler() {
                    
                    @Override
                    public PanicResult outOfMemory(long max, long used, long possible) {
                        return PanicResult.CONTINUE;
                    }
                    
                    @Override
                    public PanicResult goalNotMet(long goal, long possible) {
                        return PanicResult.CONTINUE;
                    }
                })
                .build();
        world.setLoadListener(new DummyLoadListener());
        
        world.addLoadMarker(new LoadMarker(0, 0, 0, 32, 32, 0));
        world.updateLoadMarkers().forEach((f) -> f.join());
    }
    
    @Test
    public void initTest() {
        // See init() above
    }
}
