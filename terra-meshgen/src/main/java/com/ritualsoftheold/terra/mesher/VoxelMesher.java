package com.ritualsoftheold.terra.mesher;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.mesher.resource.TextureManager;

import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * Creates a mesh based on Terra's voxel data.
 *
 */
public interface VoxelMesher {
    
    /**
     * Creates a mesh for chunk at given address.
     * @param addr
     * @param textures
     */
    void chunk(long addr, TextureManager textures);
    
    /**
     * Creates a mesh for octree at given address.
     * @param addr
     * @param reg
     * @return Address for mesh data.
     */
    void octree(long addr, MaterialRegistry reg);
    
    FloatList getVertices();
    
    IntList getIndices();
    
    FloatList getTextureCoords();

}
