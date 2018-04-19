package com.ritualsoftheold.terra.offheap.test;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.ritualsoftheold.terra.TerraModule;
import com.ritualsoftheold.terra.buffer.BlockBuffer;
import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.material.TerraMaterial;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkStorage;
import com.ritualsoftheold.terra.offheap.chunk.ChunkType;
import com.ritualsoftheold.terra.offheap.chunk.compress.EmptyChunkFormat;
import com.ritualsoftheold.terra.offheap.chunk.compress.Palette16ChunkFormat;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk.Storage;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Tests chunk buffer functionality. Not to be confused with (TODO) chunk
 * storage test.
 *
 */
public class ChunkBufferTest {
    
    private static final Memory mem = OS.memory();
    
    private ChunkBuffer buf;
    private MaterialRegistry reg;
    private TerraMaterial[] mats;
    
    @Before
    public void initBuf() {
        ChunkStorage storage = new ChunkStorage(reg, null, 0, null, null);
        
        buf = new ChunkBuffer.Builder()
                .maxChunks(64)
                .queueSize(4)
                .memListener(new DummyMemoryUseListener())
                .build(storage, (short) 1);
    }
    
    @Before
    public void initMaterials() {
        reg = new MaterialRegistry();
        TerraModule mod = new TerraModule("test");
        mats = new TerraMaterial[11];
        for (int i = 0; i < 11; i++) {
            mats[i] = mod.newMaterial().name("test" + i).build();
        }
        mod.registerMaterials(reg);
    }
    
    @Test
    public void newChunk() {
        for (int i = 0; i < 64; i++) {
            assertEquals(64 - i, buf.getFreeCapacity());
            assertEquals(i, buf.newChunk());
            assertEquals(EmptyChunkFormat.INSTANCE, buf.getChunk(i).getStorageInternal().format);
        }
        assertEquals(-1, buf.newChunk());
        assertEquals(0, buf.getFreeCapacity());
        assertEquals(5 * 64, buf.getSaveSize());
    }
    
    @Test
    public void saveLoadTest() {
        for (int i = 0; i < 64; i++) {
            assertEquals(64 - i, buf.getFreeCapacity());
            assertEquals(i, buf.newChunk());
            
            OffheapChunk chunk = buf.getChunk(i);
            long addr = mem.allocate(DataConstants.CHUNK_MAX_BLOCKS / 2);
            Storage storage = new Storage(Palette16ChunkFormat.INSTANCE, addr, DataConstants.CHUNK_MAX_BLOCKS / 2);
            chunk.setStorageInternal(storage);
            
            try (BlockBuffer buf = chunk.getBuffer()) {
                buf.write(mats[1]);
                chunk.flushChanges();
            }
        }
        
        long saveAddr = mem.allocate(buf.getSaveSize());
        buf.save(saveAddr);
        
        buf.unload(); // Crashed yet?
        
        initBuf(); // Get new chunk buffer
        buf.load(saveAddr, 64);
        
        for (int i = 0; i < 64; i++) {
            OffheapChunk chunk = buf.getChunk(i);
            try (BlockBuffer buf = chunk.getBuffer()) {
                assertEquals(mats[1].getWorldId(), buf.read().getWorldId());
            }
        }
    }
}
