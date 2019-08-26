package com.ritualsoftheold.terra.test;

import com.ritualsoftheold.terra.manager.material.TerraModule;
import com.ritualsoftheold.terra.manager.material.Registry;
import com.ritualsoftheold.terra.manager.material.TerraObject;
import com.ritualsoftheold.terra.manager.DataConstants;
import com.ritualsoftheold.terra.memory.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.memory.chunk.ChunkLArray;
import com.ritualsoftheold.terra.memory.chunk.ChunkStorage;
import com.ritualsoftheold.terra.memory.chunk.compress.Palette16ChunkFormat;
import com.ritualsoftheold.terra.memory.node.OffheapChunk;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

import org.openjdk.jmh.annotations.*;

public class ChunkPerformanceTest {

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }

    @State(Scope.Thread)
    public static class StateVars{
        public static final Memory mem = OS.memory();

        public OffheapChunk chunk;
        public Registry reg;

        public ChunkLArray chunkLArray;
        public TerraObject[] mats;
        public Byte[] chunkByteArray;


        @Setup(Level.Trial)
        public void init() {
            int queueSize = 65;
            long queueAddr = mem.allocate(queueSize * 8 * 2);
            reg = new Registry();
            ChunkStorage storage = new ChunkStorage(reg, null, 1, null, null);
            ChunkBuffer buf = new ChunkBuffer(storage, 0, 10, queueSize, new DummyMemoryUseListener(), false);
            chunk = new OffheapChunk(0, buf, queueAddr, queueAddr + queueSize * 8, queueSize);

            chunkLArray = new ChunkLArray();

            TerraModule mod = new TerraModule("test");
            mats = new TerraObject[64];
            for (int i = 0; i < 64; i++) {
                mats[i] = mod.newMaterial().name("test" + i).build();
            }
            mod.registerMaterials(reg);
            chunkByteArray = new Byte[DataConstants.CHUNK_MAX_BLOCKS];

            long addr = mem.allocate(DataConstants.CHUNK_MAX_BLOCKS / 2);
            OffheapChunk.Storage storageC = new OffheapChunk.Storage(Palette16ChunkFormat.INSTANCE, addr, DataConstants.CHUNK_MAX_BLOCKS );
            chunk.setStorageInternal(storageC);
        }
        @TearDown
        public void tearDown(){
            chunkLArray.getChunkVoxelData().free();
        }
    }
    @Benchmark @BenchmarkMode(Mode.All)
    public void TestAddOffheapChunk(StateVars stateVars){
        for (int i = 0; i < 64; i++) {
            stateVars.chunk.queueChange(i, 1);
        }
    }
    @Benchmark @BenchmarkMode(Mode.All)
    public void TestLarrayAdd(StateVars stateVars){
//        System.out.println("CHUNK ADDRESS: "+stateVars.chunkLArray.getChunkVoxelData().address());
//        System.out.println("Size: "+stateVars.chunkLArray.getChunkVoxelData().size());
        for(int i = 0; i < 64; i++){
            stateVars.chunkLArray.set(i,(byte)1);
        }

    }
    @Benchmark @BenchmarkMode(Mode.All)
    public void TestRegularAddByteArray(StateVars stateVars){
        for(int i = 0; i < 64; i++){
            stateVars.chunkByteArray[i] = 1;
        }
    }



}
