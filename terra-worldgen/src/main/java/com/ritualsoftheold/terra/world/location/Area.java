package com.ritualsoftheold.terra.world.location;

import com.ritualsoftheold.terra.world.NoiseGenerator;
import com.ritualsoftheold.terra.world.enumerators.Slope;
import com.sudoplay.joise.module.ModuleAutoCorrect;

import java.util.ArrayList;

public class Area extends ArrayList<Point>{
    private double maxHeight;
    private double minHeight;
    private long seed;
    private Slope slope;

    public Area(int maxHeight, int minHeight, Slope slope){
        this.maxHeight = maxHeight;
        this.minHeight = minHeight;
        this.slope = slope;
    }

    public void makeSeed(){
        seed = (long)(size() * maxHeight * minHeight);

        NoiseGenerator noiseGenerator = new NoiseGenerator(seed);
        ModuleAutoCorrect autoCorrect = noiseGenerator.generateNoise(minHeight, maxHeight);

        for(Point point:this){
            point.setHeight((int)autoCorrect.get((double) point.getX(), (double) point.getY()));
        }
    }
}
