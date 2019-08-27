package com.ritualsoftheold.terra.server.manager.gen.interfaces;

import com.ritualsoftheold.terra.core.buffer.BlockBuffer;
import com.ritualsoftheold.terra.manager.material.TerraObject;
import xerial.larray.LByteArray;

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

    LByteArray getLArray();
    
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
    void useMaterial(TerraObject material);
}
