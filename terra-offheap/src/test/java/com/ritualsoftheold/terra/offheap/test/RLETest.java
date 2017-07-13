package com.ritualsoftheold.terra.offheap.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.RunLengthCompressor;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Tests RunLengthCompressor.
 *
 */
public class RLETest {
    
    private static final Memory mem = OS.memory();
    
    @Test
    public void deAndCompressTest() {
        long origin = mem.allocate(DataConstants.CHUNK_UNCOMPRESSED);
        mem.setMemory(origin, DataConstants.CHUNK_UNCOMPRESSED, (byte) 0xff);
        
        long compressed = mem.allocate(DataConstants.CHUNK_UNCOMPRESSED);
        int len = RunLengthCompressor.compress(origin, compressed);
        
        long newData = mem.allocate(DataConstants.CHUNK_UNCOMPRESSED);
        RunLengthCompressor.decompress(compressed, newData);
        
        for (int i = 0; i < DataConstants.CHUNK_UNCOMPRESSED; i++) {
            assertEquals("byte: " + i, mem.readByte(origin + i), mem.readByte(newData + i));
        }
    }
}
