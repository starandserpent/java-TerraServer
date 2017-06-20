package com.ritualsoftheold.terra.offheap.test;

import org.junit.Before;
import org.junit.Test;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyChunkLoader;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyOctreeLoader;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;

/**
 * Tests OffheapWorld.
 *
 */
public class WorldTest {
    
    private OffheapWorld world;

    @Before
    public void init() {
        world = new OffheapWorld(new DummyChunkLoader(), new DummyOctreeLoader(8192), new MaterialRegistry());
    }
    
    @Test
    public void loadAreaTest() {
        world.loadArea(0, 0, 0, 100);
    }
}
