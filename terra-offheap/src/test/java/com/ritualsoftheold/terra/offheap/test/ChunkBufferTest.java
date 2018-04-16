package com.ritualsoftheold.terra.offheap.test;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkType;
import com.ritualsoftheold.terra.offheap.chunk.compress.EmptyChunkFormat;

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
    
    @Before
    public void init() {
        buf = new ChunkBuffer.Builder()
                .maxChunks(64)
                .queueSize(4)
                .memListener(new DummyMemoryUseListener())
                .build((short) 1);
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
    }
}
