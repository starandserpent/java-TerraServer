package com.ritualsoftheold.terra.offheap.world.gen;

import java.util.HashSet;
import java.util.Set;

import com.ritualsoftheold.terra.buffer.BlockBuffer;
import com.ritualsoftheold.terra.material.TerraMaterial;
import com.ritualsoftheold.terra.world.gen.GeneratorControl;

public class OffheapGeneratorControl implements GeneratorControl {
    
    private Set<TerraMaterial> materialHints;
    
    private BlockBuffer buffer;
    
    private WorldGenManager manager;
    
    private boolean end;
    
    public OffheapGeneratorControl() {
        this.materialHints = new HashSet<>();
    }
    
    @Override
    public BlockBuffer getBuffer() {
        if (buffer == null) {
            buffer = manager.createBuffer(materialHints.size());
        }
        return buffer;
    }

    @Override
    public void endPipeline() {
        end = true;
    }
    
    public boolean shouldContinue() {
        return !end;
    }

    @Override
    public void useMaterial(TerraMaterial material) {
        materialHints.add(material); // Adds only if it is not there yet
    }

}
