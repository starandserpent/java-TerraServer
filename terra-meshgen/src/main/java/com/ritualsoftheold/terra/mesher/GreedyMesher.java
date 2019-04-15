package com.ritualsoftheold.terra.mesher;

import com.ritualsoftheold.terra.core.buffer.BlockBuffer;
import com.ritualsoftheold.terra.core.material.TerraMaterial;
import com.ritualsoftheold.terra.core.material.TerraTexture;
import com.ritualsoftheold.terra.mesher.resource.TextureManager;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.data.BufferWithFormat;

/**
 * Greedy mesher does culling and try to merge same blocks into bigger faces.
 *
 */
public class GreedyMesher implements VoxelMesher {
    private CullingHelper culling;

    public GreedyMesher(CullingHelper culling) {
        this.culling = culling;
    }

    public GreedyMesher() {
        this(new CullingHelper());
    }

    @Override
    public void chunk(BlockBuffer buf, TextureManager textures, MeshContainer mesh) {
        assert buf != null;
        assert textures != null;
        assert mesh != null;

        // Generate mappings for culling
        Face[][] faces = culling.cull(buf);

        // Reset buffer to starting position
        buf.seek(0);

        for(Face[] voxel:faces){
            if(voxel != null) {
                for (Face face : voxel) {
                    if (face != null) {
                        mesh.vector(face.getVector3f());
                        mesh.triangle(face.getIndexes());
                        mesh.texture(face.getTextureCoords());
                    }
                }
            }
        }
    }

    @Override
    public void cube(int id, float scale, TextureManager textures, MeshContainer mesh) {

    }
}
