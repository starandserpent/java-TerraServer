package com.ritualsoftheold.terra.offheap.test;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkType;

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
    
    @Test
    public void queueTest() {
        // Create 3 chunks
        buf.newChunk();
        buf.newChunk();
        buf.newChunk();
        
        // Configure the chunk we use for testing
        long addr = mem.allocate(DataConstants.CHUNK_UNCOMPRESSED);
        mem.setMemory(addr, DataConstants.CHUNK_UNCOMPRESSED, (byte) 0);
        buf.setChunkType(2, ChunkType.UNCOMPRESSED);
        buf.setChunkAddr(2, addr);
        buf.setChunkLength(2, DataConstants.CHUNK_UNCOMPRESSED);
        buf.setChunkUsed(2, DataConstants.CHUNK_UNCOMPRESSED);
        
        // Verify we did stuff correctly in this test
        assertEquals(0, buf.getBlock(2, 0));
        assertEquals(0, buf.getBlock(2, 1));
        assertEquals(0, buf.getBlock(2, DataConstants.CHUNK_MAX_BLOCKS - 1));
        
        // Do changes (does it crash?)
        buf.queueChange(2, 1, (short) 3);
        buf.flushChanges();
        
        // Check that changes were made to CORRECT block
        assertEquals(0, buf.getBlock(2, 0));
        assertEquals(3, buf.getBlock(2, 1));
        assertEquals(0, buf.getBlock(2, 2));
    }
    
    @Test
    public void queueAdvancedTest() {
        int count = 5;
        for (int i = 0; i < count; i++) {
            buf.newChunk();
            
            // Configure chunks we use for testing
            long addr = mem.allocate(DataConstants.CHUNK_UNCOMPRESSED);
            mem.setMemory(addr, DataConstants.CHUNK_UNCOMPRESSED, (byte) 0);
            buf.setChunkType(i, ChunkType.UNCOMPRESSED);
            buf.setChunkAddr(i, addr);
            buf.setChunkLength(i, DataConstants.CHUNK_UNCOMPRESSED);
            buf.setChunkUsed(i, DataConstants.CHUNK_UNCOMPRESSED);
            
            // Verify we did stuff correctly in this test
            assertEquals(0, buf.getBlock(i, 0));
            assertEquals(0, buf.getBlock(i, 1));
            assertEquals(0, buf.getBlock(i, DataConstants.CHUNK_MAX_BLOCKS - 1)); // And that there is enough memory...
        }
        
        for (int i = 0; i < count; i++) {
            buf.queueChange(i, 1, (short) 3);
        }
        buf.flushChanges();
        
        // Check that changes were made to CORRECT block
        for (int i = 0; i < count; i++) {
            assertEquals(0, buf.getBlock(i, 0));
            assertEquals(3, buf.getBlock(i, 1));
            assertEquals(0, buf.getBlock(i, 2));
        }
        
        // Test saving data...
        int saveSize = buf.getSaveSize();
        long saveAddr = mem.allocate(saveSize);
        buf.save(saveAddr);
        buf.unload(); // And unloading!
        
        init(); // Get new chunk buffer!
        buf.load(saveAddr, count);
        
        // Check that loading didn't corrupt stuff
        for (int i = 0; i < count; i++) {
            assertEquals(0, buf.getBlock(i, 0));
            assertEquals(3, buf.getBlock(i, 1));
            assertEquals(0, buf.getBlock(i, 2));
        }
    }
}
