package com.ritualsoftheold.terra.world.gen;

public class EmptyWorldGenerator implements WorldGenerator<Object> {

    @Override
    public Object initialize(GenerationTask task, Pipeline<Object> pipeline) {
        return null;
    }

}
