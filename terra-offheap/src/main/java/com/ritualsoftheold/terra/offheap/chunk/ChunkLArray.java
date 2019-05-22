package com.ritualsoftheold.terra.offheap.chunk;

import com.ritualsoftheold.terra.offheap.DataConstants;
import xerial.larray.LByteArray;
import xerial.larray.japi.LArrayJ;

public class ChunkLArray {
    private float x;
    private float y;
    private float z;

    private static int CHUNK_SIZE = DataConstants.CHUNK_MAX_BLOCKS;
    private LByteArray chunkVoxelData = LArrayJ.newLByteArray(CHUNK_SIZE);

    public ChunkLArray() {
    }

    public ChunkLArray(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public byte get(int x, int y, int z){
        int idx = x + CHUNK_SIZE * (y + CHUNK_SIZE * z);
        return chunkVoxelData.apply(idx);

    }
    public byte get (int idx){
        return chunkVoxelData.getByte(idx);
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
}
