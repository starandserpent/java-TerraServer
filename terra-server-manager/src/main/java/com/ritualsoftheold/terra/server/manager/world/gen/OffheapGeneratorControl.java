package com.ritualsoftheold.terra.server.manager.world.gen;

import java.util.HashSet;
import java.util.Set;

import com.ritualsoftheold.terra.core.materials.TerraObject;
import com.ritualsoftheold.terra.memory.SelfTrackAllocator;
import xerial.larray.LByteArray;
import xerial.larray.japi.LArrayJ;

public class OffheapGeneratorControl {
    
    private Set<TerraObject> materialHints;

    private boolean end;
    
    private SelfTrackAllocator allocator;

    private LByteArray lByteArray;

    
    public OffheapGeneratorControl(SelfTrackAllocator allocator) {
        this.materialHints = new HashSet<>();
        this.end = false;
        this.allocator = allocator;

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

    public int getMemoryUsed() {
        return allocator.getMemoryUsed();
    }
}
