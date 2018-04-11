package com.ritualsoftheold.terra.world.gen;

import java.util.List;

/**
 * Pipeline of world generation allows chaining multiple methods that might
 * alter the world data.
 *
 * @param <T> Type of metadata which is passed between methods in
 * this pipeline.
 */
public abstract class Pipeline<T> {
    
    private List<TriConsumer<GenerationTask, GeneratorControl, T>> methods;
    
    /**
     * Adds a method to end of the pipeline.
     * @param method Lambda with correct signature.
     * @return This pipeline.
     */
    public Pipeline<T> addLast(TriConsumer<GenerationTask, GeneratorControl, T> method) {
        methods.add(method);
        return this;
    }
    
    /**
     * Adds a method to start of the pipeline.
     * @param method Lambda with correct signature.
     * @return This pipeline.
     */
    public Pipeline<T> addFirst(TriConsumer<GenerationTask, GeneratorControl, T> method) {
        methods.add(0, method);
        return this;
    }
    
    /**
     * Called by implementation to execute the pipeline.
     * @param task Generation task.
     * @param control Control for the operation.
     * @param meta Original metadata from initialization method.
     */
    protected abstract void execute(GenerationTask task, GeneratorControl control, T meta);
}
