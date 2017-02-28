package com.ritualsoftheold.terra.node;

import com.ritualsoftheold.terra.material.TerraMaterial;

/**
 * Simple block implementation; this merely acts as container in heap.
 * There is no good reason to save blocks offheap, usually. After all world
 * generation and other large scale operations work directly with materials
 * and their ids.
 *
 */
public class SimpleBlock implements Block {
    
    private TerraMaterial material;
    private float size;
    
    public SimpleBlock(TerraMaterial initialMat, float size) {
        this.material = initialMat;
        this.size = size;
    }
    
    @Override
    public Type getNodeType() {
        return Type.BLOCK;
    }

    @Override
    public void setMaterial(TerraMaterial mat) {
        this.material = mat;
    }

    @Override
    public TerraMaterial getMaterial() {
        return material;
    }

    @Override
    public float getSize() {
        return size;
    }

}
