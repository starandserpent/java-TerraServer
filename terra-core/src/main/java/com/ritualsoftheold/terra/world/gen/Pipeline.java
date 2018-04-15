package com.ritualsoftheold.terra.world.gen;

import java.util.ArrayList;
import java.util.List;

/**
 * Pipeline of world generation allows chaining multiple methods that might
 * alter the world data.
 *
 * @param <T> Type of metadata which is passed between methods in
 * this pipeline.
 */
public abstract class Pipeline<T> {
    
    protected List<TriConsumer<GenerationTask, GeneratorControl, T>> methods;
    
    public Pipeline() {
        this.methods = new ArrayList<>();
    }
    
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
}
