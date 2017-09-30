package com.ritualsoftheold.terra.node;

import com.ritualsoftheold.terra.material.TerraMaterial;

/**
 * Chunk is a node which contains many blocks.
 *
 */
public interface Chunk extends Node {
    
    default int getIndex(int x, int y, int z) {
        return z * 4096 + y * 64 + x;
    }
    
    short getBlockId(int index);
    
    default short getBlockId(int x, int y, int z) {
        return getBlockId(getIndex(x, y, z));
    }
    
    void setBlockId(int index, short id);
    
    default void setBlockId(int x, int y, int z, short id) {
        setBlockId(getIndex(x, y, z), id);
    }
    
    void getBlockIds(int[] indices, short[] ids);
    
    default short[] getBlockIds(int[] indices) {
        short[] ids = new short[indices.length];
        getBlockIds(indices, ids);
        return ids;
    }
    
    void setBlockIds(int[] indices, short[] ids);
    
    TerraMaterial getBlock(int index);
    
    default TerraMaterial getBlock(int x, int y, int z) {
        return getBlock(getIndex(x, y, z));
    }
    
    void setBlock(int index, TerraMaterial material);
    
    default void setBlock(int x, int y, int z, TerraMaterial material) {
        setBlock(getIndex(x, y, z), material);
    }
    
    /**
     * Gets maximum block count of this chunk.
     * @return Maximum block count.
     */
    int getMaxBlockCount();
    
    /**
     * Gets data of this chunk. Each entry represents 0.25m space in chunk,
     * so looping through it is fast.
     * 
     * Note that creating this data is potentially very expensive.
     * @return Chunk data.
     */
    default short[] getData() {
        short[] data = new short[getMaxBlockCount()];
        getData(data);
        return data;
    }
    
    /**
     * Gets data of this chunk. Each entry represents 0.25m space in chunk,
     * so looping through it is fast.
     * 
     * Note that creating this data is potentially very expensive.
     */
    void getData(short[] data);
    
    /**
     * Sets the data of this chunk. Each entry represents 0.25m space in chunk,
     * so it is easy to construct.
     * 
     * Note that setting chunk data is potentially very expensive.
     * @param data
     */
    void setData(short[] data);
}
