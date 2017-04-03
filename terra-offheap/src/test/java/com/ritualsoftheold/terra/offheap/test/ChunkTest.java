package com.ritualsoftheold.terra.offheap.test;

import org.junit.Test;

import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkUtils;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

import static org.junit.Assert.*;

/**
 * Tests everything related to offheap chunks.
 *
 */
public class ChunkTest {
    
    /**
     * If this doesn't work, then we're screwed anyway...
     */
    @SuppressWarnings("unused")
    private static Memory mem = OS.memory();
    
    @Test
    public void jvmMemoryTest() {
        short[] data = new short[DataConstants.CHUNK_MAX_BLOCKS]; // Initialize big array
        data[64 * 16 + 21] = 1;
        data[64 * 16 * 16] = 2;
        // Does this crash?
    }
    
    @Test
    public void chunkTest1() {
        ChunkBuffer buf = new ChunkBuffer(10, 1024);
        
        assertTrue(buf.hasSpace());
        
        int bufferId = buf.createChunk(DataConstants.CHUNK_MIN_SIZE);
        OffheapChunk chunk = new OffheapChunk(null, buf, bufferId);
        
        short[] data = new short[DataConstants.CHUNK_MAX_BLOCKS];
        data[21] = 1; // Set on 25cm cube to... not AIR; 21th is at 0,0,0 inside 1m block
        chunk.setData(data);
        assertEquals(1, chunk.l_getMaterial(0, 0, 0));
    }
    
    @Test
    public void chunkTest2() {
        ChunkBuffer buf = new ChunkBuffer(10, 1024);
        
        assertTrue(buf.hasSpace());
        
        int bufferId = buf.createChunk(DataConstants.CHUNK_MIN_SIZE);
        OffheapChunk chunk = new OffheapChunk(null, buf, bufferId);
        
        short[] data = new short[DataConstants.CHUNK_MAX_BLOCKS];
        data[21] = 1;
        data[85] = 2;
        data[149] = 3;
        chunk.setData(data);
        assertEquals(1, chunk.l_getMaterial(0, 0, 0));
        assertEquals(2, chunk.l_getMaterial(1, 0, 0));
        assertEquals(3, chunk.l_getMaterial(2, 0, 0));
    }
    
    @Test
    public void chunkTest3() {
        ChunkBuffer buf = new ChunkBuffer(10, 1024);
        
        assertTrue(buf.hasSpace());
        
        int bufferId = buf.createChunk(DataConstants.CHUNK_MIN_SIZE);
        OffheapChunk chunk = new OffheapChunk(null, buf, bufferId);
        
        short[] data = new short[DataConstants.CHUNK_MAX_BLOCKS];
        data[21] = 1;
        data[149] = 3;
        chunk.setData(data);
        assertEquals(1, chunk.l_getMaterial(0, 0, 0));
        assertEquals(0, chunk.l_getMaterial(1, 0, 0));
        assertEquals(3, chunk.l_getMaterial(2, 0, 0));
    }
    
    @Test
    public void chunkTest4() {
        ChunkBuffer buf = new ChunkBuffer(10, 1024);
        
        assertTrue(buf.hasSpace());
        
        int bufferId = buf.createChunk(DataConstants.CHUNK_MIN_SIZE);
        OffheapChunk chunk = new OffheapChunk(null, buf, bufferId);
        
        short[] data = new short[DataConstants.CHUNK_MAX_BLOCKS];
        data[21] = 1;
        for (int i = 0; i < 64; i++) {
            data[64 + i] = 4;
        }
        data[149] = 3;
        chunk.setData(data);
        assertEquals(1, chunk.l_getMaterial(0, 0, 0));
        assertEquals(4, chunk.l_getMaterial(1.1f, 0, 0));
        assertEquals(3, chunk.l_getMaterial(2, 0, 0));
    }
    
    @Test
    public void chunkTest5() {
        ChunkBuffer buf = new ChunkBuffer(10, 1024);
        
        assertTrue(buf.hasSpace());
        
        int bufferId = buf.createChunk(DataConstants.CHUNK_MIN_SIZE);
        OffheapChunk chunk = new OffheapChunk(null, buf, bufferId);
        
        short[] data = new short[DataConstants.CHUNK_MAX_BLOCKS];
        data[64 * 16 + 21] = 1;
        data[64 * 16 * 16 + 21] = 2;
        chunk.setData(data);
        assertEquals(1, chunk.l_getMaterial(0, 1, 0));
        //assertEquals(2, chunk.l_getMaterial(0, 0, 1));
    }
    
    @Test
    public void setDataSpeedTest() {
        ChunkBuffer buf = new ChunkBuffer(10, 1024);
        
        assertTrue(buf.hasSpace());
        
        int bufferId = buf.createChunk(DataConstants.CHUNK_MAX_BLOCKS * 2);
        OffheapChunk chunk = new OffheapChunk(null, buf, bufferId);
        
        short[] data = new short[DataConstants.CHUNK_MAX_BLOCKS];
        for (int i = 0; i < 4096; i++) {
            data[64 * i + 21] = 1;
        }
        long nanoTime = System.nanoTime();
        chunk.setData(data);
        System.out.println("setData speed: " + (System.nanoTime() - nanoTime));
    }
    
    @Test
    public void chunk025LookupTest() {
        // TODO more comprehensive test
        assertEquals(0, ChunkUtils.get025BlockIndex(-0.6f, -0.6f, -0.6f));
        assertEquals(ChunkUtils.get025BlockIndex(-0.4f, -0.1f, -0.1f), 21);
        assertEquals(63, ChunkUtils.get025BlockIndex(1f, 1f, 1f));
    }
}
