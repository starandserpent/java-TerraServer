package com.ritualsoftheold.terra.offheap.test;

import org.junit.Before;
import org.junit.Test;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyChunkLoader;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyOctreeLoader;
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
        world = new OffheapWorld(new DummyChunkLoader(), new DummyOctreeLoader(8192), new MaterialRegistry(), new EmptyWorldGenerator());
    }
    
    @Test
    public void loadAreaTest() {
        world.loadArea(0, 0, 0, 1024, new WorldLoadListener() {
            
            @Override
            public void octreeLoaded(long addr, float x, float y, float z, float scale) {
                
            }
            
            @Override
            public void chunkLoaded(long addr, float x, float y, float z) {
                
            }
        }, false);
    }
}
