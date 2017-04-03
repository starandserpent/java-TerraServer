package com.ritualsoftheold.terra.offheap.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Random;

import org.junit.Test;
import org.xerial.snappy.Snappy;

import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;

/**
 * Tests compression speed (average) and amount of compression done
 * for chunk full of random data.
 *
 */
public class CompressTest {
    
    private static final int MATERIAL_COUNT = 10;
    
    private long nano;
    private byte[] noOptimize; // To avoid JVM optimizating benchmark away
    
    @Test
    public void snappyTest() throws IOException {
        byte[] data = new byte[DataConstants.CHUNK_MAX_BLOCKS];
        for (int i = 0; i < data.length; i++) { // Create pseudo-random test data
            data[i] = (byte) new Random().nextInt(MATERIAL_COUNT);
        }
        for (int i = 0; i < 3000; i++) {
            nano = System.nanoTime();
            noOptimize = Snappy.compress(data);
        }
        long nanoTime = System.nanoTime();
        int length = Snappy.rawCompress(data, 0, data.length, new byte[DataConstants.CHUNK_MAX_BLOCKS], 0);
        System.out.println("Compression took: " + (System.nanoTime() - nanoTime) / 1000000f);
        System.out.println("Compressed size: " + length + "; " + (length * 1f / data.length) + " of original.");
    }
    
    @Test
    public void chunkPackTest() {
        short[] data = new short[DataConstants.CHUNK_MAX_BLOCKS];
        for (int i = 0; i < data.length; i++) { // Create pseudo-random test data
            data[i] = (short) new Random().nextInt(MATERIAL_COUNT);
        }
        for (int i = 0; i < 3000; i++) {
            nano = System.nanoTime();
        }
        ChunkBuffer buf = new ChunkBuffer(10, 1024);
        
        assertTrue(buf.hasSpace());
        
        int bufferId = buf.createChunk(DataConstants.CHUNK_MAX_BLOCKS * 2);
        OffheapChunk chunk = new OffheapChunk(null, buf, bufferId);
        
        long nanoTime = System.nanoTime();
        chunk.setData(data);
        System.out.println("Compression took: " + (System.nanoTime() - nanoTime) / 1000000f);
        System.out.println("Compressed size: " + chunk.l_getDataSize() + "; " + (chunk.l_getDataSize() * 1f / (data.length * 2)) + " of original.");
    }
}
