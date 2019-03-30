package com.ritualsoftheold.terra.world;

import com.sudoplay.joise.module.ModuleAutoCorrect;
import com.sudoplay.joise.module.ModuleBasisFunction;
import com.sudoplay.joise.module.ModuleFractal;

public class NoiseGenerator {

    private ModuleFractal gen;

    private static final int OCTAVES = 7;
    private static final double FREQUENCY = 0.05;

    public NoiseGenerator(long seed){
        //Creates basic fractal module
        gen = new ModuleFractal();
        gen.setAllSourceBasisTypes(ModuleBasisFunction.BasisType.GRADIENT);
        gen.setAllSourceInterpolationTypes(ModuleBasisFunction.InterpolationType.CUBIC);
        gen.setNumOctaves(OCTAVES);
        gen.setFrequency(FREQUENCY);
        gen.setType(ModuleFractal.FractalType.FBM);
        gen.setSeed(seed);
    }

    public ModuleAutoCorrect generateNoise(double low, double high){
        /*
         * ... route it through an autocorrection module...
         *
         * This module will sample it's source multiple times and attempt to
         * auto-correct the output to the range specified.
         */

        ModuleAutoCorrect ac = new ModuleAutoCorrect(low, high);
        ac.setSource(gen); // set source (can usually be either another Module or a double value; see specific module for details)
        ac.setSamples(10000); // set how many samples to take
        ac.calculate2D(); // perform the calculations
        return ac;
    }
}
