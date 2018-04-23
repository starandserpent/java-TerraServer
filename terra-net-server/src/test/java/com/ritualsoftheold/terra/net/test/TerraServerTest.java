package com.ritualsoftheold.terra.net.test;

import java.util.concurrent.ForkJoinPool;

import org.junit.Before;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.net.TerraMessages;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyChunkLoader;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyOctreeLoader;
import com.ritualsoftheold.terra.offheap.memory.MemoryPanicHandler;
import com.ritualsoftheold.terra.offheap.memory.MemoryPanicHandler.PanicResult;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;
import com.starandserpent.venom.listeners.ListenerMessageHandler;
import com.starandserpent.venom.listeners.Listeners;
import com.starandserpent.venom.server.UdpServer;

public class TerraServerTest {
    
    private OffheapWorld world;
    private UdpServer server;
    
    public void launch() {
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
        
        server = new UdpServer.Builder()
                .handlerProvider((address) -> {
                    return (conn, msg, flags) -> {
                        int testId = msg.readInt();
                        switch (testId) {
                            case 1:
                                // TODO
                        }
                    };
                })
                .build();
    }
}
