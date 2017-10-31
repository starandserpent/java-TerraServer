package com.ritualsoftheold.terra.offheap.test;

import static org.junit.Assert.*;

import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkStorage;
import com.ritualsoftheold.terra.offheap.chunk.ChunkType;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyChunkLoader;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

public class ChunkStorageTest {
    
    private static final Memory mem = OS.memory();
    
    private ChunkStorage storage;
    private MaterialRegistry registry;
    
    @Before
    public void init() {
        ChunkBuffer.Builder builder = new ChunkBuffer.Builder()
                .id((short) 1)
                .maxChunks(64)
                .globalQueue(8)
                .chunkQueue(4)
                .memListener(new DummyMemoryUseListener());
        storage = new ChunkStorage(builder, 256, new DummyChunkLoader(), Executors.newCachedThreadPool());
        registry = new MaterialRegistry();
    }
    
    @Test
    public void newChunk() {
        storage.newChunk();
        ChunkBuffer buf = storage.getBuffer(0);
        
        long addr = mem.allocate(DataConstants.CHUNK_UNCOMPRESSED);
        mem.setMemory(addr, DataConstants.CHUNK_UNCOMPRESSED, (byte) 0);
        buf.setChunkType(0, ChunkType.UNCOMPRESSED);
        buf.setChunkAddr(0, addr);
        buf.setChunkLength(0, DataConstants.CHUNK_UNCOMPRESSED);
        buf.setChunkUsed(0, DataConstants.CHUNK_UNCOMPRESSED);
        
        OffheapChunk chunk = storage.getTemporaryChunk(0, registry);
        assertNotNull(chunk);
        assertEquals(registry.getMaterial("base:air"), chunk.getBlock(0));
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
