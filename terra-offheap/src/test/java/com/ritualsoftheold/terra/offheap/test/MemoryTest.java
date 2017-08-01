package com.ritualsoftheold.terra.offheap.test;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyChunkLoader;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyOctreeLoader;
import com.ritualsoftheold.terra.offheap.memory.MemoryPanicHandler;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;
import com.ritualsoftheold.terra.world.LoadMarker;
import com.ritualsoftheold.terra.world.gen.EmptyWorldGenerator;

/**
 * Tests offheap memory management.
 * TODO implement this test after finishing in world code
 *
 */
public class MemoryTest {
    
    private OffheapWorld world;
    
    @Before
    public void init() {
        world = new OffheapWorld(new DummyChunkLoader(), new DummyOctreeLoader(8192), new MaterialRegistry(),
                new TestWorldGenerator());
        world.setLoadListener(new DummyLoadListener());
    }
    
    /**
     * Tests what happens when memory manager should free memory and fails,
     * but not critically.
     */
    @Test
    public void testFree1() {
        AtomicBoolean called = new AtomicBoolean(false);
        
        world.setMemorySettings(0, 10000000, new MemoryPanicHandler() {
            
            @Override
            public PanicResult outOfMemory(long max, long used, long possible) {
                return PanicResult.CONTINUE;
            }
            
            @Override
            public boolean handleFreeze(long stamp) {
                return true;
            }
            
            @Override
            public PanicResult goalNotMet(long goal, long possible) {
                called.set(true);
                return PanicResult.CONTINUE;
            }
        });
        
        long stamp = world.enter();
        world.addLoadMarker(new LoadMarker(0, 0, 0, 256, 256, 0));
        world.updateLoadMarkers().forEach((future) -> future.join());
        System.out.println("Load markers up to date!");
        world.requestUnload();
        world.leave(stamp);
        
        int counter = 0;
        while (called.get() != true) {
            if (counter > 5) {
                assertTrue("took too long", false);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            counter++;
        }
    }
}
