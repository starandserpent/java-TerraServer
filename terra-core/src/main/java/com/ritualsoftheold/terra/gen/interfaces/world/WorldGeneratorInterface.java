package com.ritualsoftheold.terra.gen.interfaces.world;

import com.ritualsoftheold.terra.gen.tasks.GenerationTask;
import com.ritualsoftheold.terra.gen.tasks.Pipeline;
import com.ritualsoftheold.terra.material.MaterialRegistry;

/**
 * Implementations of this generate the world from scratch.
 *
 */
public interface WorldGeneratorInterface<T> {
    
    void setup(long seed, MaterialRegistry materialRegistry);
    
    /**
     * Called first when a part of world needs to be generated.
     * @param task Generation task. This contains coordinates and other
     * useful information.
     * @param pipeline Pipeline where to register methods to be called after
     * this.
     * @return
     */
    T initialize(GenerationTask task, Pipeline<T> pipeline);
}
