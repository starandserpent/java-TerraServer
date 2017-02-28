package com.ritualsoftheold.terra.node;

import com.ritualsoftheold.terra.material.TerraMaterial;

/**
 * Block is a type of node, which has only one material.
 * Blocks are immutable in the sense that modifying block will NEVER
 * modify actual block data in world.
 *
 */
public interface Block extends Node {
    
    /**
     * Sets material of this block.
     * @param mat New material.
     */
    void setMaterial(TerraMaterial mat);
    
    /**
     * Gets material of this block.
     * @return Material.
     */
    TerraMaterial getMaterial();
    
    /**
     * Gets size (scale) of this block.
     * @return Size, in meters.
     */
    float getSize();
}
