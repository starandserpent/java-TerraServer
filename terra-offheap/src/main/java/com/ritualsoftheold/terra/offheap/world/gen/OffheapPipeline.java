package com.ritualsoftheold.terra.offheap.world.gen;

import com.ritualsoftheold.terra.core.gen.tasks.GenerationTask;
import com.ritualsoftheold.terra.core.gen.tasks.Pipeline;
import com.ritualsoftheold.terra.core.gen.interfaces.GeneratorControl;
import com.ritualsoftheold.terra.core.gen.interfaces.TriConsumer;

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
