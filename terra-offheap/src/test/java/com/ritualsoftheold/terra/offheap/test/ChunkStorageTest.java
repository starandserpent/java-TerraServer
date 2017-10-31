package com.ritualsoftheold.terra.offheap.test;

import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;

import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkStorage;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyChunkLoader;

public class ChunkStorageTest {
    
    private ChunkStorage storage;
    
    @Before
    public void init() {
        ChunkBuffer.Builder builder = new ChunkBuffer.Builder()
                .id((short) 1)
                .maxChunks(64)
                .globalQueue(8)
                .chunkQueue(4)
                .memListener(new DummyMemoryUseListener());
        storage = new ChunkStorage(builder, 256, new DummyChunkLoader(), Executors.newCachedThreadPool());
    }
    
    @Test
    public void newChunk() {
        storage.newChunk();
    }
}
