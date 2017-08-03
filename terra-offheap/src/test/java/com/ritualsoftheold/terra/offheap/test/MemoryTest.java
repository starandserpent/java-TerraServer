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
                assertTrue("wrong action", false);
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
        assertTrue("success", true);
    }
    
    /**
     * Tests freeing memory when memory manager should free memory but can't
     * and fails criticallly.
     */
    @Test
    public void testFree2() {
        AtomicBoolean called = new AtomicBoolean(false);
        
        world.setMemorySettings(0, 0, new MemoryPanicHandler() {
            
            @Override
            public PanicResult outOfMemory(long max, long used, long possible) {
                called.set(true);
                return PanicResult.CONTINUE;
            }
            
            @Override
            public boolean handleFreeze(long stamp) {
                return true;
            }
            
            @Override
            public PanicResult goalNotMet(long goal, long possible) {
                assertTrue("wrong action", false);
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
        assertTrue("success", true);
    }
    
    /**
     * Tests memory manager when no action is possible or needed.
     */
    @Test
    public void testFree3() {
        AtomicBoolean called = new AtomicBoolean(false);
        
        world.setMemorySettings(5000000, 5000000, new MemoryPanicHandler() {
            
            @Override
            public PanicResult outOfMemory(long max, long used, long possible) {
                called.set(true);
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
        
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertFalse("unnecessary action", called.get());
    }
    
    /**
     * Tests freeing memory in case where it should succeed.
     */
    @Test
    public void testFree4() {
        AtomicBoolean called = new AtomicBoolean(false);
        
        world.setMemorySettings(100000, 100000, new MemoryPanicHandler() {
            
            @Override
            public PanicResult outOfMemory(long max, long used, long possible) {
                called.set(true);
                System.out.println("Out of memory");
                return PanicResult.CONTINUE;
            }
            
            @Override
            public boolean handleFreeze(long stamp) {
                return true;
            }
            
            @Override
            public PanicResult goalNotMet(long goal, long possible) {
                called.set(true);
                System.out.println("Goal not met");
                return PanicResult.CONTINUE;
            }
        });
        
        long stamp = world.enter();
        LoadMarker marker = new LoadMarker(0, 0, 0, 256, 256, 0);
        world.addLoadMarker(marker);
        world.updateLoadMarkers().forEach((future) -> future.join());
        world.removeLoadMarker(marker);
        System.out.println("Load markers up to date!");
        world.requestUnload();
        world.leave(stamp);
        
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertFalse("free failed", called.get());
    }
}
