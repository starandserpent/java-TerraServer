package com.ritualsoftheold.terra.mesher;

import com.ritualsoftheold.terra.mesher.resource.TextureManager;
import com.ritualsoftheold.terra.offheap.chunk.ChunkLArray;
import xerial.larray.LByteArray;

import java.util.HashMap;

/**
 * Creates a mesh based on Terra's voxel data.
 *
 */
public interface VoxelMesher {
    
    /**
     * Creates a mesh for chunk data that given iterator provides.
     * @param data Block buffer for data.
     * */
    void chunk(ChunkLArray data);
    
    /**
     * Creates a mesh for a simple cube, perhaps an octree node.
     * @param id
     * @param scale
     * @param textures
     */
    void cube(int id, float scale, TextureManager textures);
}
