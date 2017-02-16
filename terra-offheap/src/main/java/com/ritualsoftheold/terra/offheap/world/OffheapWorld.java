package com.ritualsoftheold.terra.offheap.world;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.node.Octree;
import com.ritualsoftheold.terra.world.DataProvider;
import com.ritualsoftheold.terra.world.TerraWorld;

public class OffheapWorld implements TerraWorld {
    
    private DataProvider provider;
    private float chunkScale;
    
    public OffheapWorld(DataProvider provider, float chunkScale) {
        this.provider = provider;
        this.chunkScale = chunkScale;
    }
    
    @Override
    public DataProvider getDataProvider() {
        return provider;
    }

    @Override
    public void setDataProvider(DataProvider provider) throws IllegalStateException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Octree getMasterOctree() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public float getChunkScale() {
        return chunkScale;
    }

    @Override
    public Octree stripData(float x, float y, float z, float viewDistance) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MaterialRegistry getMaterialRegistry() {
        // TODO Auto-generated method stub
        return null;
    }

}
