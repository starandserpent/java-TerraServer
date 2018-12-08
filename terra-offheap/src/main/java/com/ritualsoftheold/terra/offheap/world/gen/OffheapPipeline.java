package com.ritualsoftheold.terra.offheap.world.gen;

import com.ritualsoftheold.terra.world.gen.GenerationTask;
import com.ritualsoftheold.terra.world.gen.GeneratorControl;
import com.ritualsoftheold.terra.world.gen.Pipeline;
import com.ritualsoftheold.terra.world.gen.TriConsumer;

/**
 * World generation pipeline implementation of terra-offheap.
 * 
 */
public class OffheapPipeline<T> extends Pipeline<T> {

    protected void execute(GenerationTask task, OffheapGeneratorControl control, T meta) {
        for (TriConsumer<GenerationTask, GeneratorControl, T> method : methods) {
            method.accept(task, control, meta); // World generator sets some blocks, probably
            if (!control.shouldContinue()) {
                break; // Previous method asked us to not continue
            }
        }
    }

}
