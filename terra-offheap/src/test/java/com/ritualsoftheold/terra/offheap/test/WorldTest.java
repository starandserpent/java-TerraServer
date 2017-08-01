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
        world = new OffheapWorld(new DummyChunkLoader(), new DummyOctreeLoader(8192), new MaterialRegistry(), new EmptyWorldGenerator());
        
        world.setMemorySettings(0, 0, new MemoryPanicHandler() {
            
            @Override
            public PanicResult outOfMemory(long max, long used, long possible) {
                return null;
            }
            
            @Override
            public boolean handleFreeze(long stamp) {
                return false;
            }
            
            @Override
            public PanicResult goalNotMet(long goal, long possible) {
                return null;
            }
        });
    }
    
    @Test
    public void loadAreaTest() {
        world.loadArea(0, 0, 0, 1024, new WorldLoadListener() {
            
            @Override
            public void octreeLoaded(long addr, long groupAddr, float x, float y, float z, float scale) {
                
            }
            
            @Override
            public void chunkLoaded(long addr, ChunkBuffer buf, float x, float y, float z) {
                
            }
        }, false);
    }
}
