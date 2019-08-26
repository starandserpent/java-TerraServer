package com.ritualsoftheold.terra.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.ritualsoftheold.terra.manager.material.TerraModule;
import com.ritualsoftheold.terra.core.buffer.BlockBuffer;
import com.ritualsoftheold.terra.manager.material.Registry;
import com.ritualsoftheold.terra.manager.material.TerraObject;
import com.ritualsoftheold.terra.manager.node.Node;
import com.ritualsoftheold.terra.manager.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.manager.chunk.ChunkStorage;
import com.ritualsoftheold.terra.manager.chunk.compress.Palette16ChunkFormat;
import com.ritualsoftheold.terra.manager.node.OffheapChunk;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.ritualsoftheold.terra.manager.DataConstants;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

public class ChunkTest {
    
    private static final Memory mem = OS.memory();
    
    private OffheapChunk chunk;
    private Registry reg;
    
    @Before
    public void init() {
        int queueSize = 65;
        long queueAddr = mem.allocate(queueSize * 8 * 2);
        reg = new Registry();
        ChunkStorage storage = new ChunkStorage(reg, null, 1, null, null);
        ChunkBuffer buf = new ChunkBuffer(storage, 0, 10, queueSize, new DummyMemoryUseListener(), false);
        chunk = new OffheapChunk(0, buf, queueAddr, queueAddr + queueSize * 8, queueSize);
    }
    
    @Test
    public void basicsTest() {
        assertEquals(Node.Type.CHUNK, chunk.getNodeType());
        try {
            chunk.getBuffer();
        } catch (UnsupportedOperationException e) { // Empty chunk type
            assertTrue(true);
            return;
        }
        assertFalse(true);
    }
    
    @Test
    public void storageLeakTest() {
        try (OffheapChunk.Storage storage = chunk.getStorage()) {
            // Do nothing here
        }
        assertEquals(0, chunk.getStorageInternal().getUserCount());
    }
    
    @Test
    public void queueFailTest() {
        // 10 first fit to the queue
        for (int i = 0; i < 10; i++) {
            chunk.queueChange(0, 10);
        }
        
        // The one after them, doesn't
        try {
            chunk.queueChange(0, 100);
        } catch (UnsupportedOperationException e) {
            assertTrue(true);
            return;
        }
        assertFalse(true);
    }
    
    @Test
    public void palette16Test() {
        long addr = mem.allocate(DataConstants.CHUNK_MAX_BLOCKS / 2);
        OffheapChunk.Storage storage = new OffheapChunk.Storage(Palette16ChunkFormat.INSTANCE, addr, DataConstants.CHUNK_MAX_BLOCKS );
        chunk.setStorageInternal(storage);
        
        // Create 11 new materials
        TerraModule mod = new TerraModule("test");
        TerraObject[] mats = new TerraObject[11];
        for (int i = 0; i < 11; i++) {
            mats[i] = mod.newMaterial().name("test" + i).build();
        }
        mod.registerMaterials(reg);
        
        // 10 first fit to the queue
        for (int i = 0; i < 10; i++) {
            chunk.queueChange(i, mats[i].getWorldId());
        }
        
        // The one after them, flushes them
        chunk.queueChange(10, mats[10].getWorldId());
        
        // Check chunk data
        BlockBuffer buf = chunk.getBuffer();
        for (int i = 0; i < 10; i++) {
            Assert.assertEquals(mats[i].getWorldId(), buf.read().getWorldId());
            buf.next();
        }
        
        // 10 more queries forces flush
        for (int i = 0; i < 10; i++) {
            chunk.queueChange(i, mats[i].getWorldId());
        }
        
        // Check data... again
        buf.seek(0);
        for (int i = 0; i < 11; i++) {
            Assert.assertEquals(mats[i].getWorldId(), buf.read().getWorldId());
            buf.next();
        }
    }
    
    @Test
    public void queueStress() {
        long addr = mem.allocate(DataConstants.CHUNK_MAX_BLOCKS / 2);
        OffheapChunk.Storage storage = new OffheapChunk.Storage(Palette16ChunkFormat.INSTANCE, addr, DataConstants.CHUNK_MAX_BLOCKS );
        chunk.setStorageInternal(storage);
        
        // Create 11 new materials
        TerraModule mod = new TerraModule("test");
        TerraObject[] mats = new TerraObject[64];
        for (int i = 0; i < 64; i++) {
            mats[i] = mod.newMaterial().name("test" + i).build();
        }
        mod.registerMaterials(reg);
        
        for (int i = 0; i < 64; i++) {
            chunk.queueChange(i, mats[i].getWorldId());
        }
    }
    
    @Test
    public void refTest() {
        chunk.setRef(0, "foo");
        assertEquals("foo", chunk.getRef(0));
    }
}
