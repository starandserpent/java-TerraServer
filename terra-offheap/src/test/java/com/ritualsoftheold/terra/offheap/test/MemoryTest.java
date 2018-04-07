package com.ritualsoftheold.terra.offheap.test;

import static org.junit.Assert.*;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyChunkLoader;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyOctreeLoader;
import com.ritualsoftheold.terra.offheap.memory.MemoryPanicHandler;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;
import com.ritualsoftheold.terra.offheap.world.WorldLoadListener;
import com.ritualsoftheold.terra.world.LoadMarker;
import com.ritualsoftheold.terra.world.gen.EmptyWorldGenerator;

/**
 * Tests offheap memory management.
 * TODO implement this test after finishing in world code
 *
 */
public class MemoryTest {
    
    private ChunkBuffer.Builder bufferBuilder;
    
    @Before
    public void init() {
        bufferBuilder = new ChunkBuffer.Builder()
                .maxChunks(128)
                .globalQueue(8)
                .queueSize(4);
    }
    
    /**
     * Tests what happens when memory manager should free memory and fails,
     * but not critically.
     */
    @Test
    public void testFree1() {
        AtomicBoolean called = new AtomicBoolean(false);
        OffheapWorld world = new OffheapWorld.Builder()
                .chunkLoader(new DummyChunkLoader())
                .octreeLoader(new DummyOctreeLoader(32768))
                .storageExecutor(ForkJoinPool.commonPool())
                .chunkStorage(bufferBuilder, 128)
                .octreeStorage(32768)
                .generator(new TestWorldGenerator())
                .generatorExecutor(ForkJoinPool.commonPool())
                .materialRegistry(new MaterialRegistry())
                .memorySettings(0, 10000000, new MemoryPanicHandler() {
                    
                    @Override
                    public PanicResult outOfMemory(long max, long used, long possible) {
                        assertTrue("wrong action", false);
                        return PanicResult.CONTINUE;
                    }
                    
                    @Override
                    public PanicResult goalNotMet(long goal, long possible) {
                        called.set(true);
                        return PanicResult.CONTINUE;
                    }
                })
                .build();
        AtomicInteger loadedCount = new AtomicInteger(0);
        world.setLoadListener(new WorldLoadListener() {
            
            @Override
            public void octreeLoaded(long addr, long groupAddr, int id, float x,
                    float y, float z, float scale, LoadMarker trigger) {
                loadedCount.incrementAndGet();
            }
            
            @Override
            public void chunkLoaded(OffheapChunk chunk, float x, float y, float z,
                    LoadMarker trigger) {
                
            }
        });
        
        world.addLoadMarker(new LoadMarker(0, 0, 0, 256, 256, 0));
        world.updateLoadMarkers().forEach((future) -> future.join());
        System.out.println("Load markers up to date!");
        world.requestUnload();
        System.out.println("loaded: " + loadedCount);
        
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
        
        OffheapWorld world = new OffheapWorld.Builder()
                .chunkLoader(new DummyChunkLoader())
                .octreeLoader(new DummyOctreeLoader(32768))
                .storageExecutor(ForkJoinPool.commonPool())
                .chunkStorage(bufferBuilder, 128)
                .octreeStorage(32768)
                .generator(new TestWorldGenerator())
                .generatorExecutor(ForkJoinPool.commonPool())
                .materialRegistry(new MaterialRegistry())
                .memorySettings(10, 10, new MemoryPanicHandler() {
            
                    @Override
                    public PanicResult outOfMemory(long max, long used, long possible) {
                        called.set(true);
                        return PanicResult.CONTINUE;
                    }
                    
                    @Override
                    public PanicResult goalNotMet(long goal, long possible) {
                        assertTrue("wrong action", false);
                        return PanicResult.CONTINUE;
                    }
                })
                .build();
        world.setLoadListener(new DummyLoadListener());
        
        world.addLoadMarker(new LoadMarker(0, 0, 0, 256, 256, 0));
        world.updateLoadMarkers().forEach((future) -> future.join());
        System.out.println("Load markers up to date!");
        world.requestUnload();
        
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
        
        OffheapWorld world = new OffheapWorld.Builder()
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
                        called.set(true);
                        return PanicResult.CONTINUE;
                    }
                    
                    @Override
                    public PanicResult goalNotMet(long goal, long possible) {
                        called.set(true);
                        return PanicResult.CONTINUE;
                    }
                })
                .build();
        world.setLoadListener(new DummyLoadListener());
        
        world.addLoadMarker(new LoadMarker(0, 0, 0, 256, 256, 0));
        world.updateLoadMarkers().forEach((future) -> future.join());
        System.out.println("Load markers up to date!");
        world.requestUnload();
        
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
        
        OffheapWorld world = new OffheapWorld.Builder()
                .chunkLoader(new DummyChunkLoader())
                .octreeLoader(new DummyOctreeLoader(32768))
                .storageExecutor(ForkJoinPool.commonPool())
                .chunkStorage(bufferBuilder, 128)
                .octreeStorage(32768)
                .generator(new TestWorldGenerator())
                .generatorExecutor(ForkJoinPool.commonPool())
                .materialRegistry(new MaterialRegistry())
                .memorySettings(1100000, 10000000, new MemoryPanicHandler() {
                    
                    @Override
                    public PanicResult outOfMemory(long max, long used, long possible) {
                        called.set(true);
                        System.out.println("Out of memory");
                        return PanicResult.CONTINUE;
                    }
                    
                    @Override
                    public PanicResult goalNotMet(long goal, long possible) {
                        called.set(true);
                        System.out.println("Goal not met; goal: " + goal + ", possible: " + possible);
                        return PanicResult.CONTINUE;
                    }
                })
                .build();
        world.setLoadListener(new DummyLoadListener());
        
        LoadMarker marker = new LoadMarker(0, 0, 0, 256, 256, 0);
        world.addLoadMarker(marker);
        world.updateLoadMarkers().forEach((future) -> future.join());
        world.removeLoadMarker(marker);
        System.out.println("Load markers up to date!");
        world.requestUnload();
        
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertFalse("free failed", called.get());
    }
}
