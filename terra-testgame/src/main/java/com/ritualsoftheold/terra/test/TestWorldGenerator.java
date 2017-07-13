package com.ritualsoftheold.terra.test;

import java.util.Arrays;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.world.gen.WorldGenerator;

public class TestWorldGenerator implements WorldGenerator {

    @Override
    public boolean initialize(long seed, MaterialRegistry materialRegistry) {
        return true;
    }

    @Override
    public boolean generate(short[] data, float x, float y, float z, float scale) {
        System.out.println("chunk: " + x + "," + y + "," + z);
        //if (y > 0 && y < 32) {
            Arrays.fill(data, (short) 1);
        //}
        
        return true;
    }

}
