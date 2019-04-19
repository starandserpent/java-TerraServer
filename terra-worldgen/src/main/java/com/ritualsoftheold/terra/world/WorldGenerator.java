package com.ritualsoftheold.terra.world;

import com.ritualsoftheold.terra.core.buffer.BlockBuffer;
import com.ritualsoftheold.terra.core.gen.interfaces.GeneratorControl;
import com.ritualsoftheold.terra.core.gen.interfaces.world.WorldGeneratorInterface;
import com.ritualsoftheold.terra.core.gen.tasks.GenerationTask;
import com.ritualsoftheold.terra.core.gen.tasks.Pipeline;
import com.ritualsoftheold.terra.core.material.MaterialRegistry;
import com.ritualsoftheold.terra.core.material.TerraMaterial;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.world.location.Area;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class WorldGenerator implements WorldGeneratorInterface<Void> {

    private TerraMaterial dirt;
    private TerraMaterial air;
    private TerraMaterial grass;
    private ArrayList<Area> areas;
    private int index;

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
        System.out.println("x:" + task.getX() + " y:" + task.getY() + " z:" + task.getZ());
        if (task.getY() < 0) {
            for (int i = 0; i < 2; i++) {
                buf.next();
            }
            for (int i = 0; i < 64; i++) {
                buf.write(dirt);
                buf.next();
            }
            for (int i = 0; i < DataConstants.CHUNK_MAX_BLOCKS; i++) {
                buf.write(grass);
                buf.next();
            }
        }else {
            for (int i = 0; i < DataConstants.CHUNK_MAX_BLOCKS; i++) {
                buf.write(grass);
                buf.next();
            }
        }
        index++;
        System.out.println(index);
    }
}