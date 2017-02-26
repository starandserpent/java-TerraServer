package com.ritualsoftheold.terra.node;

import com.ritualsoftheold.terra.material.TerraMaterial;

/**
 * Block is a type of node, which has only one material.
 * Blocks are immutable in the sense that modifying block will NEVER
 * modify actual block data in world.
 *
 */
public interface Block extends Node {
    
    void setMaterial(TerraMaterial mat);
    
    TerraMaterial getMaterial();
}
