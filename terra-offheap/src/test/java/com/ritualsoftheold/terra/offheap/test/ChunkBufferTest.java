package com.ritualsoftheold.terra.offheap.test;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;

/**
 * Tests chunk buffer functionality. Not to be confused with (TODO) chunk
 * storage test.
 *
 */
public class ChunkBufferTest {
    
    private ChunkBuffer buf;
    
    @Before
    public void init() {
        buf = new ChunkBuffer.Builder()
                .id((short) 1)
                .maxChunks(64)
                .globalQueue(8)
                .chunkQueue(4)
                .memListener(new DummyMemoryUseListener())
                .build();
    }
    
    @Test
    public void newChunk() {
        for (int i = 0; i < 64; i++) {
            assertEquals(64 - i, buf.getFreeCapacity());
            assertEquals(i, buf.newChunk());
        }
        assertEquals(-1, buf.newChunk());
        assertEquals(0, buf.getFreeCapacity());
    }
    
    @Test
    public void addrsTest() {
        int index = buf.newChunk();
        
        // At the beginning, check that default values are correct
        assertEquals(0, buf.getChunkAddr(index));
        assertEquals(1, buf.getChunkType(index));
        assertEquals(0, buf.getChunkLength(index));
        assertEquals(0, buf.getChunkUsed(index));
        
        // Then modify said values (not real values)
        buf.setChunkAddr(index, 1);
        buf.setChunkType(index, (byte) 2);
        buf.setChunkLength(index, 4);
        buf.setChunkUsed(index, 3);
        
        // ... check that modifications were correct
        assertEquals(1, buf.getChunkAddr(index));
        assertEquals(2, buf.getChunkType(index));
        assertEquals(4, buf.getChunkLength(index));
        assertEquals(3, buf.getChunkUsed(index));
    }
}
