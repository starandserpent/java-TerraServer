package com.ritualsoftheold.terra.world.gen;

/**
 * Implementations of this generate the world from scratch.
 *
 */
public interface WorldGenerator<T> {
    
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
