package com.ritualsoftheold.terra.world;

import com.ritualsoftheold.terra.core.material.MaterialRegistry;
import com.ritualsoftheold.terra.core.gen.tasks.GenerationTask;
import com.ritualsoftheold.terra.core.gen.tasks.Pipeline;
import com.ritualsoftheold.terra.core.gen.interfaces.world.WorldGeneratorInterface;

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
