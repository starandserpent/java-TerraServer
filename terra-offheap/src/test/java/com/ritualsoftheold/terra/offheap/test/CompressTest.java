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
    
    private static final int MATERIAL_COUNT = 20;
    private static final int WARMUP = 60000;
    
    private long nano;
    private byte[] noOptimize; // To avoid JVM optimizating benchmark away
    
    @Test
    public void snappyTest() throws IOException {
        System.out.println("\nTesting Snappy, full random:");
        byte[] data = new byte[DataConstants.CHUNK_MAX_BLOCKS];
        for (int i = 0; i < data.length; i++) { // Create pseudo-random test data
            data[i] = (byte) new Random().nextInt(MATERIAL_COUNT);
        }
        for (int i = 0; i < WARMUP; i++) {
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
        System.out.println("\nTesting chunk setData(), full random");
        short[] data = new short[DataConstants.CHUNK_MAX_BLOCKS];
        for (int i = 0; i < data.length; i++) { // Create pseudo-random test data
            data[i] = (short) new Random().nextInt(MATERIAL_COUNT);
        }
        
        ChunkBuffer buf = new ChunkBuffer(10, 1024);
        
        assertTrue(buf.hasSpace());
        
        int bufferId = buf.createChunk(DataConstants.CHUNK_MAX_BLOCKS * 2);
        OffheapChunk chunk = new OffheapChunk(null, buf, bufferId);
        
        for (int i = 0; i < WARMUP; i++) {
            nano = System.nanoTime();
            chunk.setData(data);
        }
        
        long nanoTime = System.nanoTime();
        chunk.setData(data);
        System.out.println("Compression took: " + (System.nanoTime() - nanoTime) / 1000000f);
        System.out.println("Compressed size: " + chunk.l_getDataSize() + "; " + (chunk.l_getDataSize() * 1f / (data.length * 2)) + " of original.");
    }
    
    @Test
    public void chunkPack05Test() {
        System.out.println("\nTesting chunk setData(), 0.5m random");
        short[] data = new short[DataConstants.CHUNK_MAX_BLOCKS];
        for (int i = 0; i < data.length / 8; i += 8) { // Create pseudo-random test data
            short rand = (short) new Random().nextInt(MATERIAL_COUNT);
            data[i] = rand;
            data[i + 1] = rand;
            data[i + 2] = rand;
            data[i + 3] = rand;
            data[i + 4] = rand;
            data[i + 5] = rand;
            data[i + 6] = rand;
            data[i + 7] = rand;
        }
        
        ChunkBuffer buf = new ChunkBuffer(10, 1024);
        
        assertTrue(buf.hasSpace());
        
        int bufferId = buf.createChunk(DataConstants.CHUNK_MAX_BLOCKS * 2);
        OffheapChunk chunk = new OffheapChunk(null, buf, bufferId);
        
        for (int i = 0; i < WARMUP; i++) {
            nano = System.nanoTime();
            chunk.setData(data);
        }
        
        long nanoTime = System.nanoTime();
        chunk.setData(data);
        System.out.println("Compression took: " + (System.nanoTime() - nanoTime) / 1000000f);
        System.out.println("Compressed size: " + chunk.l_getDataSize() + "; " + (chunk.l_getDataSize() * 1f / (data.length * 2)) + " of original.");
    }
    
    @Test
    public void snappy05Test() throws IOException {
        System.out.println("\nTesting Snappy, 0.5m random:");
        byte[] data = new byte[DataConstants.CHUNK_MAX_BLOCKS];
        for (int i = 0; i < data.length / 8; i += 8) { // Create pseudo-random test data
            byte rand = (byte) new Random().nextInt(MATERIAL_COUNT);
            data[i] = rand;
            data[i + 1] = rand;
            data[i + 2] = rand;
            data[i + 3] = rand;
            data[i + 4] = rand;
            data[i + 5] = rand;
            data[i + 6] = rand;
            data[i + 7] = rand;
        }
        for (int i = 0; i < WARMUP; i++) {
            nano = System.nanoTime();
            noOptimize = Snappy.compress(data);
        }
        long nanoTime = System.nanoTime();
        int length = Snappy.rawCompress(data, 0, data.length, new byte[DataConstants.CHUNK_MAX_BLOCKS], 0);
        System.out.println("Compression took: " + (System.nanoTime() - nanoTime) / 1000000f);
        System.out.println("Compressed size: " + length + "; " + (length * 1f / data.length) + " of original.");
    }
    
    @Test
    public void chunkPack1Test() {
        System.out.println("\nTesting chunk setData(), 1m random");
        short[] data = new short[DataConstants.CHUNK_MAX_BLOCKS];
        for (int i = 0; i < data.length / 64; i += 64) { // Create pseudo-random test data
            short rand = (short) new Random().nextInt(MATERIAL_COUNT);
            for (int j = 0; j < 64; j++) {
                data[i + j] = rand;
            }
        }
        
        ChunkBuffer buf = new ChunkBuffer(10, 1024);
        
        assertTrue(buf.hasSpace());
        
        int bufferId = buf.createChunk(DataConstants.CHUNK_MAX_BLOCKS * 2);
        OffheapChunk chunk = new OffheapChunk(null, buf, bufferId);
        
        for (int i = 0; i < WARMUP; i++) {
            nano = System.nanoTime();
            chunk.setData(data);
        }
        
        long nanoTime = System.nanoTime();
        chunk.setData(data);
        System.out.println("Compression took: " + (System.nanoTime() - nanoTime) / 1000000f);
        System.out.println("Compressed size: " + chunk.l_getDataSize() + "; " + (chunk.l_getDataSize() * 1f / (data.length * 2)) + " of original.");
    }
    
    @Test
    public void snappy1Test() throws IOException {
        System.out.println("\nTesting Snappy, 0.5m random:");
        byte[] data = new byte[DataConstants.CHUNK_MAX_BLOCKS];
        for (int i = 0; i < data.length / 64; i += 64) { // Create pseudo-random test data
            byte rand = (byte) new Random().nextInt(MATERIAL_COUNT);
            for (int j = 0; j < 64; j++) {
                data[i + j] = rand;
            }
        }
        for (int i = 0; i < WARMUP; i++) {
            nano = System.nanoTime();
            noOptimize = Snappy.compress(data);
        }
        long nanoTime = System.nanoTime();
        int length = Snappy.rawCompress(data, 0, data.length, new byte[DataConstants.CHUNK_MAX_BLOCKS], 0);
        System.out.println("Compression took: " + (System.nanoTime() - nanoTime) / 1000000f);
        System.out.println("Compressed size: " + length + "; " + (length * 1f / data.length) + " of original.");
    }
}
