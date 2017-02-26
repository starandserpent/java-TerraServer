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
    
    public SimpleBlock(TerraMaterial initialMat) {
        this.material = initialMat;
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

}
