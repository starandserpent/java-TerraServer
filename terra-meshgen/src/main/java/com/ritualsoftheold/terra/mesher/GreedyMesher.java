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

        byte[] hidden = new byte[DataConstants.CHUNK_MAX_BLOCKS]; // Visibility mappings for cubes

        // Generate mappings for culling
        culling.cull(buf, hidden);

        // Reset buffer to starting position
        buf.seek(0);

        int block = 0;
        int verticeIndex = 0;
        while (buf.hasNext()) {
            TerraMaterial material = buf.read();
            buf.next();
            if (material.getWorldId() == 1 || material.getTexture() == null) { // TODO better AIR check
                block++;
                continue;
            }

            byte faces = hidden[block]; // Read hidden faces of this block
            if (faces == 0b00111111) { // TODO better "is-air" check
                block++; // To next block!
                continue; // AIR or all faces are hidden
            }

            TerraTexture texture = material.getTexture();

            // Calculate current block position (normalized by shaders)
            int z = block / 4096; // Integer division: current z index
            int y = (block - 4096 * z) / 64;
            int x = block % 64;

            if ((faces & 0b00100000) == 0) { // LEFT
                mesh.vertex(x, y, z + 1);
                mesh.vertex(x, y + 1, z + 1);
                mesh.vertex(x, y + 1, z);
                mesh.vertex(x, y, z);


                mesh.triangle(verticeIndex, 0, 2, 3);
                mesh.triangle( verticeIndex, 2, 0, 1);

                verticeIndex += 4;

                /*
                mesh.texture(z, y);
                mesh.texture(z , y + 1);
                mesh.texture(z + 1, y + 1);
                mesh.texture(z , y + 1);
                 */
            } if ((faces & 0b00010000) ==  0) { // RIGHT
                mesh.vertex(x + 1, y, z);
                mesh.vertex(x + 1, y + 1, z);
                mesh.vertex(x + 1, y + 1, z + 1);
                mesh.vertex(x + 1, y, z + 1);


                mesh.triangle(verticeIndex, 0, 2, 3);
                mesh.triangle( verticeIndex, 2, 0, 1);

                verticeIndex += 4;
             /*   mesh.texture(z, y);
                mesh.texture(z , y + 1);
                mesh.texture(z + 1, y + 1);
                mesh.texture(z , y + 1);
              */

            } if ((faces & 0b00001000) == 0) { // UP
                //System.out.println("Draw UP");
                mesh.vertex(x, y + 1, z);
                mesh.vertex(x, y + 1, z + 1);
                mesh.vertex(x + 1, y + 1, z + 1);
                mesh.vertex(x + 1, y + 1, z);


                mesh.triangle(verticeIndex, 0, 2, 3);
                mesh.triangle( verticeIndex, 2, 0, 1);

                verticeIndex += 4;

                mesh.texture(x , z);
                mesh.texture(x , z + 1);
                mesh.texture(x + 1, z + 1);
                mesh.texture(x , z + 1);
            } if ((faces & 0b00000100) == 0) { // DOWN
                mesh.vertex(x, y, z + 1);
                mesh.vertex(x, y, z);
                mesh.vertex(x + 1, y, z);
                mesh.vertex(x + 1, y, z + 1);


                mesh.triangle(verticeIndex, 0, 2, 3);
                mesh.triangle( verticeIndex, 2, 0, 1);

                verticeIndex += 4;

               /*mesh.texture(x , z);
                mesh.texture(x , z + 1);
                mesh.texture(x + 1, z + 1);
                mesh.texture(x , z + 1);*/
            } if ((faces & 0b00000010) == 0) { // BACK
                mesh.vertex(x + 1, y, z + 1);
                mesh.vertex(x + 1, y + 1, z + 1);
                mesh.vertex(x, y + 1, z + 1);
                mesh.vertex(x, y, z + 1);

                mesh.triangle(verticeIndex, 0, 2, 3);
                mesh.triangle( verticeIndex, 2, 0, 1);

                verticeIndex += 4;

             /*   mesh.texture(z , y);
                mesh.texture(z , y + 1);
                mesh.texture(z + 1, y + 1);
                mesh.texture(z , y + 1);*/
            } if ((faces & 0b00000001) == 0) { // FRONT
                mesh.vertex(x, y, z);
                mesh.vertex(x, y + 1, z);
                mesh.vertex(x + 1, y + 1, z);
                mesh.vertex(x + 1, y, z);


                mesh.triangle(verticeIndex, 0, 2, 3);
                mesh.triangle( verticeIndex, 2, 0, 1);

                verticeIndex += 4;

               /* mesh.texture(x , y);
                mesh.texture(x , y + 1);
                mesh.texture(x + 1, y + 1);
                mesh.texture(x , y + 1);*/
            }
            block++; // Go to next block
            mesh.setTextures(x, y, z, texture);
        }
    }

    @Override
    public void cube(int id, float scale, TextureManager textures, MeshContainer mesh) {

    }
}
