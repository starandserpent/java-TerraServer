package com.ritualsoftheold.terra.offheap.test;

import org.junit.Test;

import com.ritualsoftheold.terra.offheap.DataConstants;

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
        // TODO
    }
}
