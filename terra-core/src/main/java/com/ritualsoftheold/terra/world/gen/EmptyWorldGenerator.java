package com.ritualsoftheold.terra.world.gen;

import com.ritualsoftheold.terra.material.MaterialRegistry;

public class EmptyWorldGenerator implements WorldGenerator {

    @Override
    public boolean initialize(long seed, MaterialRegistry materialRegistry) {
        return true;
    }

    @Override
    public boolean generate(short[] data, float x, float y, float z,
            float scale, WorldGenerator.Metadata meta) {
        meta.materialCount = 0; // Empty world, after all
        return true;
    }

}
