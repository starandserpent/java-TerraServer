package com.ritualsoftheold.terra.world.gen;

import com.ritualsoftheold.terra.buffer.BlockBuffer;
import com.ritualsoftheold.terra.material.TerraMaterial;

/**
 * Allows world generator to interact with Terra's implementation.
 *
 */
public interface GeneratorControl {
    
    /**
     * Acquire a block buffer for write access.
     * @return Block buffer where data should be written.
     */
    BlockBuffer getBuffer();
    
    /**
     * Later methods in pipeline will be ignored this time, if there
     * are any.
     */
    void endPipeline();
    
    /**
     * Hints Terra that given material will likely be used at some point.
     * Hints are most effective when done before any method in the pipeline
     * acquires the block buffer.
     * @param material Material hint.
     */
    void useMaterial(TerraMaterial material);
}
