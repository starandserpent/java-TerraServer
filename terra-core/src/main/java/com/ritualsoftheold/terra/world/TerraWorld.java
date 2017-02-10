package com.ritualsoftheold.terra.world;

import com.ritualsoftheold.terra.node.Octree;

/**
 * Represents a single Terra world.
 *
 */
public interface TerraWorld {
    
    /**
     * Gets data provider of this world.
     * @return Data provider.
     */
    DataProvider getDataProvider();
    
    /**
     * Sets data provider of this world. Might not work always...
     * @param provider New data provider
     * @throws IllegalStateException If provider could not be changed.
     */
    void setDataProvider(DataProvider provider) throws IllegalStateException;
    
    /**
     * Gets master octree of this world. Do not cache the result, as
     * it might change.
     * @return Master octree.
     */
    Octree getMasterOctree();
    
    /**
     * Gets scale of octree, whose children will be chunks or blocks, not
     * octrees or blocks. 32 is recommended value, but not always the case.
     * @return
     */
    float getChunkScale();
    
    // Low level, optional API. Better performance, but not for general usage
    
    void low_getOctree(long[] data, int uint_index);
    
    long low_getChunkPtr(int uint_index);
    
    void low_getChunk(long[] data, long ptr);
}
