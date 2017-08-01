package com.ritualsoftheold.terra.offheap.test;

import java.util.Arrays;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.world.gen.WorldGenerator;

public class TestWorldGenerator implements WorldGenerator {
    
    private short dirt;
    private short grass;
    
    @Override
    public boolean initialize(long seed, MaterialRegistry materialRegistry) {
        dirt = materialRegistry.getMaterial("testgame:dirt").getWorldId();
        grass = materialRegistry.getMaterial("testgame:grass").getWorldId();
        System.out.println("Initialized TestWorldGenerator: " + dirt + "; " + grass);
        return true;
    }

    @Override
    public boolean generate(short[] data, float x, float y, float z, float scale) {
        System.out.println("chunk: " + x + "," + y + "," + z);
        if (y < 0) {
            Arrays.fill(data, 0, DataConstants.CHUNK_MAX_BLOCKS / 2, dirt);
            Arrays.fill(data, DataConstants.CHUNK_MAX_BLOCKS / 2, DataConstants.CHUNK_MAX_BLOCKS, grass);
        }
        
        return true;
    }

}
