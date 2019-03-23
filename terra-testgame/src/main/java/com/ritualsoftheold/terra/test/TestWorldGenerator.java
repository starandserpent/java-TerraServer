package com.ritualsoftheold.terra.test;

import com.ritualsoftheold.terra.buffer.BlockBuffer;
import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.material.TerraMaterial;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.world.gen.GenerationTask;
import com.ritualsoftheold.terra.world.gen.GeneratorControl;
import com.ritualsoftheold.terra.world.gen.Pipeline;
import com.ritualsoftheold.terra.world.gen.WorldGenerator;

public class TestWorldGenerator implements WorldGenerator<Void> {

    // Material definitions.
    private TerraMaterial dirt;
    private TerraMaterial grass;
    
    @Override
    public void setup(long seed, MaterialRegistry materialRegistry) {
        dirt = materialRegistry.getMaterial("testgame:dirt");
        grass = materialRegistry.getMaterial("testgame:grass");
    }

    // Initialize world generation pipeline.
    @Override
    public Void initialize(GenerationTask task, Pipeline<Void> pipeline) {
        pipeline.addLast(this::generate);
        
        return null;
    }

    // World generator.
    public void generate(GenerationTask task, GeneratorControl control, Void nothing) {
        BlockBuffer buf = control.getBuffer();
        
        if (task.getY() < 0) {
            for (int i = 0; i < DataConstants.CHUNK_MAX_BLOCKS / 2; i++) {
                buf.write(dirt); // Set material of current block.
                buf.next();      // Move to next block in the buffer.
            }
            for (int i = 0; i < DataConstants.CHUNK_MAX_BLOCKS / 2; i++) {
                buf.write(grass);
                buf.next();
            }
        }
    }

}
