package com.ritualsoftheold.terra.core.gen.interfaces.world;

import com.ritualsoftheold.terra.core.TerraModule;
import com.ritualsoftheold.terra.core.gen.tasks.GenerationTask;
import com.ritualsoftheold.terra.core.gen.tasks.Pipeline;
import com.ritualsoftheold.terra.core.material.MaterialRegistry;

/**
 * Implementations of this generate the world from scratch.
 *
 */
public interface WorldGeneratorInterface<T> {
    
    void setup(MaterialRegistry materialRegistry, TerraModule mod);

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
