package com.ritualsoftheold.terra.mesher;

import com.ritualsoftheold.terra.mesher.resource.TextureManager;
import com.ritualsoftheold.terra.offheap.chunk.iterator.ChunkIterator;

import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * Creates a mesh based on Terra's voxel data.
 *
 */
public interface VoxelMesher {
    
    /**
     * Creates a mesh for chunk at given address.
     * @param it
     * @param textures
     */
    void chunk(ChunkIterator it, TextureManager textures);
    
    /**
     * Creates a mesh for a simple cube, perhaps an octree node.
     * @param id
     * @param scale
     * @param textures
     */
    void cube(short id, float scale, TextureManager textures);
    
    FloatList getVertices();
    
    IntList getIndices();
    
    FloatList getTextureCoords();

}
