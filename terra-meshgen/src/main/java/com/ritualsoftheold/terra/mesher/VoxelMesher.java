package com.ritualsoftheold.terra.mesher;

import com.ritualsoftheold.terra.material.MaterialRegistry;

/**
 * Creates a mesh based on Terra's voxel data.
 *
 */
public interface VoxelMesher {
    
    /**
     * Creates a mesh for chunk at given address.
     * @param addr
     * @param reg
     * @return Address for mesh data.
     */
    long chunk(long addr, MaterialRegistry reg);
    
    /**
     * Creates a mesh for octree at given address.
     * @param addr
     * @param reg
     * @return Address for mesh data.
     */
    long octree(long addr, MaterialRegistry reg);
    
    /**
     * Releases an address which methods in this mesher returned. This might
     * not actually free the memory - re-using it might make sense
     * concerning performance.
     * @param addr Address for mesh data.
     */
    void release(long addr);
}
