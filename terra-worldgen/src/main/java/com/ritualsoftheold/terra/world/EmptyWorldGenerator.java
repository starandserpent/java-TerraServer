package com.ritualsoftheold.terra.world;

import com.ritualsoftheold.terra.gen.tasks.GenerationTask;
import com.ritualsoftheold.terra.gen.tasks.Pipeline;
import com.ritualsoftheold.terra.gen.interfaces.world.WorldGeneratorInterface;
import com.ritualsoftheold.terra.material.MaterialRegistry;

public class EmptyWorldGenerator implements WorldGeneratorInterface<Object> {

    @Override
    public Object initialize(GenerationTask task, Pipeline<Object> pipeline) {
        return null;
    }

    @Override
    public void setup(long seed, MaterialRegistry materialRegistry) {
        // Do nothing
    }

}
