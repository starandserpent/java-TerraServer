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
    public void testChunkBuffer() {
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
    public void chunk025LookupTest() {
        // TODO more comprehensive test
        assertEquals(0, ChunkUtils.get025BlockIndex(-0.6f, -0.6f, -0.6f));
        assertEquals(ChunkUtils.get025BlockIndex(-0.4f, -0.1f, -0.1f), 21);
        assertEquals(63, ChunkUtils.get025BlockIndex(1f, 1f, 1f));
    }
}
