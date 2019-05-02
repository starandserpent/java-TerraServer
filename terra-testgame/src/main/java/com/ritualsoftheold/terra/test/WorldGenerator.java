package com.ritualsoftheold.terra.test;

import com.ritualsoftheold.terra.core.buffer.BlockBuffer;
import com.ritualsoftheold.terra.core.gen.interfaces.GeneratorControl;
import com.ritualsoftheold.terra.core.gen.interfaces.world.WorldGeneratorInterface;
import com.ritualsoftheold.terra.core.gen.tasks.GenerationTask;
import com.ritualsoftheold.terra.core.gen.tasks.Pipeline;
import com.ritualsoftheold.terra.core.material.MaterialRegistry;
import com.ritualsoftheold.terra.core.material.TerraMaterial;
import com.ritualsoftheold.terra.offheap.DataConstants;

public class WorldGenerator implements WorldGeneratorInterface<Void> {

    private TerraMaterial dirt;
    private TerraMaterial air;
    private TerraMaterial grass;

    @Override
    public void setup(long seed, MaterialRegistry materialRegistry) {
        dirt = materialRegistry.getMaterial("testgame:dirt");
        grass = materialRegistry.getMaterial("testgame:grass");
    }

    @Override
    public Void initialize(GenerationTask task, Pipeline<Void> pipeline) {
        pipeline.addLast(this::generate);
        return null;
    }

    public void generate(GenerationTask task, GeneratorControl control, Void nothing) {
        BlockBuffer buf = control.getBuffer();
        System.out.println("x:" + task.getX() + " y:" + task.getY() + " z:" + task.getZ());
        for (int i = 0; i < DataConstants.CHUNK_MAX_BLOCKS; i++) {
            buf.write(dirt);
            buf.next();
        }
    }
}