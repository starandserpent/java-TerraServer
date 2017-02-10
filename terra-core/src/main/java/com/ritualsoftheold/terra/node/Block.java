package com.ritualsoftheold.terra.node;

import com.ritualsoftheold.terra.material.TerraMaterial;

/**
 * Block is a type of node, which has only one material
 *
 */
public interface Block extends Node {
    
    void setMaterial(TerraMaterial mat);
    
    TerraMaterial getMaterial();
}
