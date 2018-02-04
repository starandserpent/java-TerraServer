package com.ritualsoftheold.terra.mesher;

import com.ritualsoftheold.terra.mesher.resource.TextureManager;
import com.ritualsoftheold.terra.offheap.chunk.iterator.ChunkIterator;

/**
 * Creates a mesh based on Terra's voxel data.
 *
 */
public interface VoxelMesher {
    
    /**
     * Creates a mesh for chunk data that given iterator provides.
     * @param it Chunk iterator.
     * @param textures Texture manager.
     */
    void chunk(ChunkIterator it, TextureManager textures, MeshContainer mesh);
    
    /**
     * Creates a mesh for a simple cube, perhaps an octree node.
     * @param id
     * @param scale
     * @param textures
     */
    void cube(short id, float scale, TextureManager textures, MeshContainer mesh);
}
