package com.ritualsoftheold.terra.jmh.meshgen;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import com.ritualsoftheold.terra.mesher.MeshContainer;
import com.ritualsoftheold.terra.mesher.NaiveMesher;
import com.ritualsoftheold.terra.mesher.resource.TextureManager;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.ChunkType;
import com.ritualsoftheold.terra.offheap.chunk.iterator.ChunkIterator;

import io.netty.buffer.ByteBufAllocator;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Uses JMH to benchmark meshers bundled with Terra.
 *
 */
public class MesherBenchmark {
    
    private static final Memory mem = OS.memory();
    
    @State(Scope.Thread)
    public static class ChunkData {
        public long chunkData;
        public TextureManager textures;
        public MeshContainer container;
        
        public ChunkData() {
            long addr = mem.allocate(DataConstants.CHUNK_UNCOMPRESSED);
            mem.setMemory(addr, DataConstants.CHUNK_UNCOMPRESSED, (byte) 0);
            mem.writeShort(addr, (short) 1); // Add some stuff to chunk
            mem.writeShort(addr + 2, (short) 0xffff);
            mem.writeShort(addr + 4, (short) 1);
            mem.writeShort(addr + 6, (short) 0xffff);
            mem.writeShort(addr + 8, (short) 1);
            mem.writeShort(addr + 10, (short) 0xffff);
            mem.writeShort(addr + 12, (short) 1);
            mem.writeShort(addr + 14, (short) 0xffff);
            mem.writeShort(addr + 16, (short) 1);
            mem.writeShort(addr + 18, (short) 0xffff);
            mem.writeShort(addr + 20, (short) 1);
            mem.writeShort(addr + 22, (short) 0xffff);
            
            chunkData = addr;
            
            textures = new TextureManager(null);
        }
    }
    
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void naiveMesherBadContainer(ChunkData data) {
        NaiveMesher mesher = new NaiveMesher();
        data.container = new MeshContainer(100, ByteBufAllocator.DEFAULT);
        mesher.chunk(ChunkIterator.forChunk(data.chunkData, ChunkType.RLE_2_2), data.textures, data.container);
        data.container.release();
    }
    
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void naiveMesherGoodContainer(ChunkData data) {
        NaiveMesher mesher = new NaiveMesher();
        data.container = new MeshContainer(10000, ByteBufAllocator.DEFAULT);
        mesher.chunk(ChunkIterator.forChunk(data.chunkData, ChunkType.RLE_2_2), data.textures, data.container);
        data.container.release();
    }
}
