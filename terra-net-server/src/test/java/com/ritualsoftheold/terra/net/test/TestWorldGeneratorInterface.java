package com.ritualsoftheold.terra.net.test;

import com.ritualsoftheold.terra.buffer.BlockBuffer;
import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.material.TerraMaterial;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.world.gen.GenerationTask;
import com.ritualsoftheold.terra.world.gen.GeneratorControl;
import com.ritualsoftheold.terra.world.gen.Pipeline;
import com.ritualsoftheold.terra.world.gen.WorldGeneratorInterface;

public class TestWorldGeneratorInterface implements WorldGeneratorInterface<Void> {
    
    private TerraMaterial dirt;
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

        for (int i = 0; i < DataConstants.CHUNK_MAX_BLOCKS / 2; i++) {
            buf.write(dirt);
            buf.next();
        }
        for (int i = 0; i < DataConstants.CHUNK_MAX_BLOCKS / 2; i++) {
            buf.write(grass);
            buf.next();
        }
    }

}
