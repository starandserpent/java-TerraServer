package com.ritualsoftheold.terra.net.test;

import static org.junit.Assert.assertFalse;

import java.util.concurrent.ForkJoinPool;

import com.ritualsoftheold.terra.memory.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.manager.material.MaterialRegistry;
import com.ritualsoftheold.terra.manager.memory.MemoryPanicHandler;
import com.ritualsoftheold.terra.net.TerraProtocol;
import com.ritualsoftheold.terra.net.client.TerraClient;
import com.ritualsoftheold.terra.manager.world.OffheapWorld;
import com.ritualsoftheold.terra.manager.world.WorldLoadListener;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.junit.Before;
import org.junit.Test;

import com.ritualsoftheold.terra.manager.io.dummy.DummyChunkLoader;
import com.ritualsoftheold.terra.manager.io.dummy.DummyOctreeLoader;
import com.ritualsoftheold.terra.memory.node.OffheapChunk;
import com.ritualsoftheold.terra.manager.world.LoadMarker;

import io.aeron.Aeron;

public class TerraClientTest {
    
    private OffheapWorld world;
    private TerraClient client;
    
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
                .generator(null)
                .generatorExecutor(ForkJoinPool.commonPool())
                .materialRegistry(new MaterialRegistry()) // TODO server sends this
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
                .perNodeReadyCheck(true)
                .build();
        
        client = new TerraClient(world, new BackoffIdleStrategy(100, 10, 1000, 100000));
    }
    
    @Test
    public void receiveTest() {
        Aeron aeron = Aeron.connect(new Aeron.Context());
        client.subscribe(aeron, "aeron:udp?endpoint=localhost:12345", TerraProtocol.DEFAULT_AERON_STREAM);
        
        LoadMarker marker = world.createLoadMarker(0, 10, 0, 32, 32, 0);
        world.addLoadMarker(marker);
        world.setLoadListener(new WorldLoadListener() {
            
            @Override
            public void octreeLoaded(long addr, long groupAddr, int id, float x,
                    float y, float z, float scale, LoadMarker trigger) {
                System.out.println("Octree2, addr: " + addr + ", scale: " + scale);
            }
            
            @Override
            public void chunkLoaded(OffheapChunk chunk, float x, float y, float z,
                    LoadMarker trigger) {
                System.out.println("Chunk2: " + chunk.getIndex());
            }
        });
        world.updateLoadMarkers();
    }
}
