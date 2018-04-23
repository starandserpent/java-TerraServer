package com.ritualsoftheold.terra.world.gen;

import com.ritualsoftheold.terra.material.MaterialRegistry;

public class EmptyWorldGenerator implements WorldGenerator<Object> {

    @Override
    public Object initialize(GenerationTask task, Pipeline<Object> pipeline) {
        return null;
    }

    @Override
    public void setup(long seed, MaterialRegistry materialRegistry) {
        // Do nothing
    }

}
