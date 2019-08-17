package com.ritualsoftheold.terra.offheap.chunk;

import com.ritualsoftheold.terra.core.material.Registry;
import com.ritualsoftheold.terra.core.material.TerraObject;
import com.ritualsoftheold.terra.offheap.DataConstants;
import xerial.larray.LByteArray;
import xerial.larray.japi.LArrayJ;

public class ChunkLArray {
    public final float x;
    public final float y;
    public final float z;

    private boolean isDifferent;
    private Registry reg;

    public static int CHUNK_SIZE = DataConstants.CHUNK_MAX_BLOCKS;
    private LByteArray chunkVoxelData;

    public ChunkLArray(float x, float y, float z, Registry reg){
        this(x, y, z, LArrayJ.newLByteArray(CHUNK_SIZE), reg);
    }

    public ChunkLArray(float x, float y, float z, LByteArray chunkVoxelData, Registry reg) {
        this.x = x;
        this.y = y;
        this.z = z;

        this.reg = reg;
        this.chunkVoxelData = chunkVoxelData;
    }

    public byte get(int x, int y, int z){
        int idx = x + (y * CHUNK_SIZE) + (z * CHUNK_SIZE * CHUNK_SIZE);
        return chunkVoxelData.apply(idx);

    }
    public TerraObject get (int idx){
        return reg.getForWorldId((int)chunkVoxelData.getByte(idx));
    }
    public void set(int x, int y, int z, byte data){
        int idx = x + CHUNK_SIZE * (y + CHUNK_SIZE * z);
        chunkVoxelData.update(idx, data);
    }
    public void set(int idx, byte data){
        chunkVoxelData.update(idx, data);
    }

    public LByteArray getChunkVoxelData() {
        return chunkVoxelData;
    }

    public void setDifferent(boolean different){
        isDifferent = different;
    }

    public boolean isDifferent() {
        return isDifferent;
    }
}
