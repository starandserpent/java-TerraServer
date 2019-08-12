package com.ritualsoftheold.terra.test;

import static org.junit.Assert.*;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;

import com.jme3.scene.Geometry;
import com.ritualsoftheold.terra.core.material.Registry;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkStorage;
//import com.ritualsoftheold.terra.offheap.io.dummy.DummyChunkLoaderInterface;
import com.ritualsoftheold.terra.offheap.io.ChunkLoader;
import org.junit.Before;
import org.junit.Test;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

public class ChunkStorageTest {
    
    private static final Memory mem = OS.memory();

    private ChunkStorage storage;
    private Registry registry;

    @Before
    public void init() {
        ChunkBuffer.Builder builder = new ChunkBuffer.Builder()
                .maxChunks(64)
                .queueSize(4)
                .memListener(new DummyMemoryUseListener());
        var mat = new com.jme3.material.Material();
        BlockingQueue<Geometry> geometryBlockingQueue = new ArrayBlockingQueue<Geometry>(1024);
        BlockingQueue<String> geoDeletQueue = new ArrayBlockingQueue<String>(1024);
        var meshListner = new com.ritualsoftheold.testgame.generation.MeshListener(mat,geometryBlockingQueue,geoDeletQueue);
        storage = new ChunkStorage(null, builder, 256, new ChunkLoader(meshListner), Executors.newCachedThreadPool());
        registry = new Registry();
    }

    @Test
    public void getLoad() {
        // Test that getBuffer and getOrLoadBuffer do their own things correctly
        assertNull(storage.getBuffer(0));
        assertNull(storage.getBuffer(0));
        assertNotNull(storage.getOrLoadBuffer(0));
        assertNotNull(storage.getBuffer(0));
    }

    @Test
    public void ensureLoadedTest() {
        storage.ensureLoaded(0);
        assertNotNull(storage.getBuffer(0));
    }
}
