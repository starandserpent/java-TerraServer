package com.ritualsoftheold.terra.offheap.world.gen;

import java.util.HashSet;
import java.util.Set;

import com.ritualsoftheold.terra.offheap.material.TerraObject;
import com.ritualsoftheold.terra.offheap.memory.SelfTrackAllocator;
import com.ritualsoftheold.terra.offheap.gen.interfaces.GeneratorControl;
import com.ritualsoftheold.terra.offheap.data.CriticalBlockBuffer;
import xerial.larray.LByteArray;
import xerial.larray.japi.LArrayJ;

public class OffheapGeneratorControl implements GeneratorControl {
    
    private Set<TerraObject> materialHints;
    
    private CriticalBlockBuffer buffer;
    
    private boolean end;
    
    private SelfTrackAllocator allocator;

    private LByteArray lByteArray;

    
    public OffheapGeneratorControl(SelfTrackAllocator allocator) {
        this.materialHints = new HashSet<>();
        this.buffer = null;
        this.end = false;
        this.allocator = allocator;

        lByteArray = LArrayJ.newLByteArray(262144);
    }
    
    @Override
    public CriticalBlockBuffer getBuffer() {
        if (buffer == null) {
           // buffer = manager.createBuffer(materialHints.size(), allocator);
        }
        return buffer;
    }

    @Override
    public LByteArray getLArray() {
        return lByteArray;
    }

    @Override
    public void endPipeline() {
        end = true;
    }
    
    public boolean shouldContinue() {
        return !end;
    }

    @Override
    public void useMaterial(TerraObject material) {
        materialHints.add(material); // Adds only if it is not there yet
    }

    public int getMemoryUsed() {
        return allocator.getMemoryUsed();
    }
}
