package com.ritualsoftheold.terra.test;

import static org.junit.Assert.*;

import java.util.concurrent.Executors;

import com.ritualsoftheold.terra.core.material.MaterialRegistry;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkStorage;
//import com.ritualsoftheold.terra.offheap.io.dummy.DummyChunkLoaderInterface;
import org.junit.Before;
import org.junit.Test;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

public class ChunkStorageTest {
    
//    private static final Memory mem = OS.memory();
//
//    private ChunkStorage storage;
//    private MaterialRegistry registry;
//
//    @Before
//    public void init() {
//        ChunkBuffer.Builder builder = new ChunkBuffer.Builder()
//                .maxChunks(64)
//                .queueSize(4)
//                .memListener(new DummyMemoryUseListener());
//        storage = new ChunkStorage(null, builder, 256, new DummyChunkLoaderInterface(), Executors.newCachedThreadPool());
//        registry = new MaterialRegistry();
//    }
//
//    @Test
//    public void getLoad() {
//        // Test that getBuffer and getOrLoadBuffer do their own things correctly
//        assertNull(storage.getBuffer(0));
//        assertNull(storage.getBuffer(0));
//        assertNotNull(storage.getOrLoadBuffer(0));
//        assertNotNull(storage.getBuffer(0));
//    }
//
//    @Test
//    public void ensureLoadedTest() {
//        storage.ensureLoaded(0);
//        assertNotNull(storage.getBuffer(0));
//    }
}
