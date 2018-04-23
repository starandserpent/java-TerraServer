package com.ritualsoftheold.terra.net.test;

import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ForkJoinPool;

import org.junit.Before;
import org.junit.Test;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.net.TerraMessages;
import com.ritualsoftheold.terra.net.client.ChunkMsgHandler;
import com.ritualsoftheold.terra.net.client.OctreeMsgHandler;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyChunkLoader;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyOctreeLoader;
import com.ritualsoftheold.terra.offheap.memory.MemoryPanicHandler;
import com.ritualsoftheold.terra.offheap.memory.MemoryPanicHandler.PanicResult;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;
import com.starandserpent.venom.client.UdpClient;
import com.starandserpent.venom.listeners.ListenerMessageHandler;
import com.starandserpent.venom.listeners.Listeners;

public class TerraClientTest {
    
    private OffheapWorld world;
    private UdpClient client;
    
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
                .build();
        
        Listeners listeners = new Listeners(TerraMessages.getTypes());
        listeners.register(TerraMessages.OCTREE_DELIVERY, new OctreeMsgHandler(world.getOctreeStorage(), world.createVerifier()));
        listeners.register(TerraMessages.CHUNK_DELIVERY, new ChunkMsgHandler(world.getChunkStorage(), world.createVerifier()));
        ListenerMessageHandler handler = listeners.getHandler();
        client = new UdpClient.Builder()
                .handler(handler)
                .build();
    }
    
    @Test
    public void receiveTest() {
        try {
            client.connect(new InetSocketAddress(InetAddress.getLocalHost(), 1234));
        } catch (IOException e) {
            assertFalse(true);
        }
    }
}
