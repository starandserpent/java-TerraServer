package com.ritualsoftheold.game.server.test;

/*import com.ritualsoftheold.terra.offheap.world.gen.GenerationTask;
import com.ritualsoftheold.terra.offheap.world.gen.GeneratorControl;
import com.ritualsoftheold.terra.offheap.world.gen.Pipeline;
import com.ritualsoftheold.terra.offheap.world.gen.WorldGeneratorInterface;

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
        
        if (task.getY() < 0) {
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

}
*/