package com.ritualsoftheold.terra.offheap.test;

import org.junit.Test;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyChunkLoader;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyOctreeLoader;
import com.ritualsoftheold.terra.offheap.memory.MemoryPanicHandler;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;

/**
 * Enlarges a world to see if it works.
 *
 */
public class EnlargeWorldTest {

    @Test
    public void enlargeTest() {
        OffheapWorld world = new OffheapWorld(new DummyChunkLoader(), new DummyOctreeLoader(8192), new MaterialRegistry(), new TestWorldGenerator());
        world.setLoadListener(new DummyLoadListener());
        world.setMemorySettings(1000000, 1000000, new MemoryPanicHandler() {

            @Override
            public PanicResult goalNotMet(long goal, long possible) {
                return null;
            }

            @Override
            public PanicResult outOfMemory(long max, long used, long possible) {
                return null;
            }

            @Override
            public boolean handleFreeze(long stamp) {
                return false;
            }
            
        });
        
        long stamp = world.enter();
        world.loadArea(0, 10, 0, 40, new DummyLoadListener(), false);
        world.leave(stamp);
        stamp = world.enter();
        world.loadArea(0, 10, 0, 40, new DummyLoadListener(), false);
        world.leave(stamp);
    }
}
