package com.ritualsoftheold.terra.manager.world.gen;

import java.util.HashSet;
import java.util.Set;

import com.ritualsoftheold.terra.core.materials.TerraObject;
import xerial.larray.LByteArray;
import xerial.larray.japi.LArrayJ;

public class OffheapGeneratorControl {
    
    private Set<TerraObject> materialHints;

    private boolean end;

    private LByteArray lByteArray;

    
    public OffheapGeneratorControl() {
        this.materialHints = new HashSet<>();
        this.end = false;

        lByteArray = LArrayJ.newLByteArray(262144);
    }

    public LByteArray getLArray() {
        return lByteArray;
    }

    public void endPipeline() {
        end = true;
    }
    
    public boolean shouldContinue() {
        return !end;
    }

    public void useMaterial(TerraObject material) {
        materialHints.add(material); // Adds only if it is not there yet
    }

}
