package com.ritualsoftheold.terra.world.test;

import com.ritualsoftheold.terra.core.buffer.BlockBuffer;
import com.ritualsoftheold.terra.core.gen.interfaces.GeneratorControl;
import com.ritualsoftheold.terra.core.gen.tasks.GenerationTask;
import com.ritualsoftheold.terra.core.material.MaterialRegistry;
import com.ritualsoftheold.terra.core.material.TerraMaterial;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.world.FileWorldLoader;
import com.ritualsoftheold.terra.world.location.Area;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class DummyWorldGenerator {


    private TerraMaterial dirt;
    private TerraMaterial air;
    private TerraMaterial grass;
    private ArrayList<Area> areas;
    private int index;

    public DummyWorldGenerator(MaterialRegistry materialRegistry) {
        dirt = materialRegistry.getMaterial("testgame:dirt");
        grass = materialRegistry.getMaterial("testgame:grass");
        air = materialRegistry.getMaterial("base:air");
    }

    public BlockBuffer generate(BlockBuffer buf) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < DataConstants.CHUNK_MAX_BLOCKS; i++) {
            int id = random.nextInt(10);
            if(id == 2) {
                buf.write(grass);
            }else if (id==1) {
                buf.write(dirt);
            }
            buf.next();
        }
        return buf;
    }
}
