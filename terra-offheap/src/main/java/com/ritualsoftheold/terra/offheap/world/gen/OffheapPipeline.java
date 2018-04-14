package com.ritualsoftheold.terra.offheap.world.gen;

import com.ritualsoftheold.terra.world.gen.GenerationTask;
import com.ritualsoftheold.terra.world.gen.GeneratorControl;
import com.ritualsoftheold.terra.world.gen.Pipeline;
import com.ritualsoftheold.terra.world.gen.TriConsumer;

public class OffheapPipeline<T> extends Pipeline<T> {

    protected void execute(GenerationTask task, OffheapGeneratorControl control, T meta) {
        for (TriConsumer<GenerationTask, GeneratorControl, T> method : methods) {
            method.accept(task, control, meta);
            if (!control.shouldContinue()) {
                break; // Previous method asked us to not continue
            }
        }
    }

}
