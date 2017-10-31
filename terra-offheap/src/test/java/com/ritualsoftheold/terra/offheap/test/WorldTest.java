package com.ritualsoftheold.terra.offheap.test;

import org.junit.Before;
import org.junit.Test;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyChunkLoader;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyOctreeLoader;
import com.ritualsoftheold.terra.offheap.memory.MemoryPanicHandler;
import com.ritualsoftheold.terra.offheap.memory.MemoryPanicHandler.PanicResult;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;
import com.ritualsoftheold.terra.offheap.world.WorldLoadListener;
import com.ritualsoftheold.terra.world.gen.EmptyWorldGenerator;

/**
 * Tests OffheapWorld.
 *
 */
public class WorldTest {
    
    private OffheapWorld world;

    @Before
    public void init() {
        world = new OffheapWorld.Builder()
                .build(); // BIG TODO
    }
}
