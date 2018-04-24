package com.ritualsoftheold.terra.net.test;

import java.net.UnknownHostException;
import java.util.concurrent.ForkJoinPool;

import org.junit.Before;

import com.ritualsoftheold.terra.TerraModule;
import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.material.TerraTexture;
import com.ritualsoftheold.terra.net.TerraMessages;
import com.ritualsoftheold.terra.net.server.SendingLoadListener;
import com.ritualsoftheold.terra.net.server.WorldObserver;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyChunkLoader;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyOctreeLoader;
import com.ritualsoftheold.terra.offheap.memory.MemoryPanicHandler;
import com.ritualsoftheold.terra.offheap.memory.MemoryPanicHandler.PanicResult;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;
import com.ritualsoftheold.terra.world.LoadMarker;
import com.ritualsoftheold.terra.world.gen.WorldGenerator;
import com.starandserpent.venom.listeners.ListenerMessageHandler;
import com.starandserpent.venom.listeners.Listeners;
import com.starandserpent.venom.server.UdpServer;

import io.netty.buffer.ByteBufAllocator;

public class TerraServerTest {
    
    private OffheapWorld world;
    private UdpServer server;
    
    public void launch() {
        ChunkBuffer.Builder bufferBuilder = new ChunkBuffer.Builder()
                .maxChunks(128)
                .queueSize(4);
        
        TerraModule mod = new TerraModule("testgame");
        mod.newMaterial().name("dirt");
        mod.newMaterial().name("grass");
        MaterialRegistry reg = new MaterialRegistry();
        mod.registerMaterials(reg);
        
        WorldGenerator<?> gen = new TestWorldGenerator();
        gen.setup(0, reg);
        
        world = new OffheapWorld.Builder()
                .chunkLoader(new DummyChunkLoader())
                .octreeLoader(new DummyOctreeLoader(32768))
                .storageExecutor(ForkJoinPool.commonPool())
                .chunkStorage(bufferBuilder, 128)
                .octreeStorage(32768)
                .generator(gen)
                .generatorExecutor(ForkJoinPool.commonPool())
                .materialRegistry(reg) // TODO server sends this
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
        
        SendingLoadListener listener = new SendingLoadListener(world, ByteBufAllocator.DEFAULT);
        world.setLoadListener(listener);
        
        server = new UdpServer.Builder()
                .handlerProvider((address) -> {
                    return (conn, msg, flags) -> {
                        LoadMarker marker = new LoadMarker(0, 10, 0, 32, 32, 0);
                        world.addLoadMarker(marker);
                        
                        WorldObserver observer = new WorldObserver(marker, conn, 10);
                        listener.addObserver(observer);
                        
                        world.updateLoadMarkers();
                    };
                })
                .build();
        try {
            server.listen(1234);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String... args) {
        new TerraServerTest().launch();
    }
}
