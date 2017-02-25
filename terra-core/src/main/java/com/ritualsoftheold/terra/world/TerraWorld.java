package com.ritualsoftheold.terra.world;

import com.ritualsoftheold.terra.material.MaterialRegistry;
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
    
    /**
     * Gets material registry that is used with this world.
     * @return Material registry.
     */
    MaterialRegistry getMaterialRegistry();
}
