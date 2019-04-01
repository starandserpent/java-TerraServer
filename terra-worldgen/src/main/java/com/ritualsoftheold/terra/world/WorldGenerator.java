package com.ritualsoftheold.terra.world;

import com.ritualsoftheold.terra.buffer.BlockBuffer;
import com.ritualsoftheold.terra.gen.interfaces.world.WorldGeneratorInterface;
import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.material.TerraMaterial;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.gen.tasks.GenerationTask;
import com.ritualsoftheold.terra.gen.interfaces.GeneratorControl;
import com.ritualsoftheold.terra.gen.tasks.Pipeline;
import com.ritualsoftheold.terra.world.FileWorldLoader;
import com.ritualsoftheold.terra.world.location.Area;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class WorldGenerator implements WorldGeneratorInterface<Void> {
    
    private TerraMaterial dirt;
    private TerraMaterial air;
    private TerraMaterial grass;
    private ArrayList<Area> areas;
    
    @Override
    public void setup(long seed, MaterialRegistry materialRegistry) {
        dirt = materialRegistry.getMaterial("testgame:dirt");
        grass = materialRegistry.getMaterial("testgame:grass");
        air = materialRegistry.getMaterial("base:air");
        File file = new File("./terra-worldgen/src/main/resources/map.png");
        FileWorldLoader loader = new FileWorldLoader(file);
        try {
            areas = loader.loadWorld();
            for(Area area:areas){
                area.makeSeed();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public Void initialize(GenerationTask task, Pipeline<Void> pipeline) {
        pipeline.addLast(this::generate);
        return null;
    }
    
    public void generate(GenerationTask task, GeneratorControl control, Void nothing) {
            BlockBuffer buf = control.getBuffer();
        for (int i = 0; i < DataConstants.CHUNK_MAX_BLOCKS; i++) {
            if(i == 1){
                buf.write(grass);
            }else if(i == 2){
                buf.write(grass);
            } else {
                buf.next();
            }
        }
    }
}
