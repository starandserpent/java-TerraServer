package com.ritualsoftheold.terra.net.test;

import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.LockSupport;

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
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;
import com.ritualsoftheold.terra.offheap.world.WorldLoadListener;
import com.ritualsoftheold.terra.world.LoadMarker;
import com.starandserpent.venom.MessageHandler;
import com.starandserpent.venom.NetMagicValues;
import com.starandserpent.venom.UdpConnection;
import com.starandserpent.venom.client.UdpClient;
import com.starandserpent.venom.listeners.ListenerMessageHandler;
import com.starandserpent.venom.listeners.Listeners;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

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
                .perNodeReadyCheck(true)
                .build();
        
        Listeners listeners = new Listeners(TerraMessages.getTypes());
        listeners.register(TerraMessages.OCTREE_DELIVERY, new OctreeMsgHandler(world.getOctreeStorage(), world.createVerifier()));
        listeners.register(TerraMessages.CHUNK_DELIVERY, new ChunkMsgHandler(world.getChunkStorage(), world.createVerifier()));
        ListenerMessageHandler handler = listeners.getHandler();
        client = new UdpClient.Builder()
                .handler(new MessageHandler() {
                    
                    @Override
                    public void receive(UdpConnection conn, ByteBuf msg, byte flags) {
                        System.out.println(msg);
                    }
                })
                .build();
    }
    
    @Test
    public void receiveTest() {
        System.out.println("init ok");
        try {
            client.connect(new InetSocketAddress(InetAddress.getLocalHost(), 1234));
            client.getConnection().sendMessage(ByteBufAllocator.DEFAULT.buffer(), NetMagicValues.NO_FLAGS);
        } catch (IOException e) {
            assertFalse(true);
        }
        
        LockSupport.parkNanos(5000000000L);
        
        LoadMarker marker = new LoadMarker(0, 10, 0, 32, 32, 0);
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
