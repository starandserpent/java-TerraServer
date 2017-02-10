package com.ritualsoftheold.terra.node;

/**
 * Chunk is a node which contains many blocks.
 *
 */
public interface Chunk {
    
    Block getBlockAt(float x, float y, float z);
    
    void setBlockAt(float x, float y, float z, Block block);
    
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
    default char[] getData() {
        char[] data = new char[getMaxBlockCount()];
        getData(data);
        return data;
    }
    
    /**
     * Gets data of this chunk. Each entry represents 0.25m space in chunk,
     * so looping through it is fast.
     * 
     * Note that creating this data is potentially very expensive.
     */
    void getData(char[] data);
    
    /**
     * Sets the data of this chunk. Each entry represents 0.25m space in chunk,
     * so it is easy to construct.
     * 
     * Note that setting chunk data is potentially very expensive.
     * @param data
     */
    void setData(char[] data);
}
