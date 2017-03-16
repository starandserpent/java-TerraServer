package com.ritualsoftheold.terra.offheap.test;

import org.junit.Test;

import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
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
    }
}
