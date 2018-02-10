package com.ritualsoftheold.terra.mesher;

import com.ritualsoftheold.terra.material.TerraTexture;
import com.ritualsoftheold.terra.mesher.resource.TextureManager;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.iterator.ChunkIterator;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * Naive mesher does culling, but doesn't try to merge same blocks into bigger faces.
 *
 */
public class NaiveMesher implements VoxelMesher {

    public NaiveMesher() {
    }

    @Override
    public void chunk(ChunkIterator it, TextureManager textures, MeshContainer mesh) {
        assert it != null;
        assert textures != null;
        assert mesh != null;

        byte[] hidden = new byte[DataConstants.CHUNK_MAX_BLOCKS]; // Visibility mappings for cubes
        //Arrays.fill(hidden, (byte) 0);

        // Generate mappings for culling
        while (!it.isDone()) {
            int begin = it.getOffset();
            short blockId = it.nextMaterial();
            if (blockId == 0) { // TODO better AIR check
                continue;
            }

            //System.out.println("begin: " + begin);
            for (int i = 0; i < it.getCount(); i++) { // Loop blocks from what we just read
                int index = begin + i;

                int rightIndex = index - 1;
                if (rightIndex > -1 && index % 64 != 0)
                    hidden[rightIndex] |= 0b00010000; // RIGHT
                int leftIndex = index + 1;
                if (leftIndex < DataConstants.CHUNK_MAX_BLOCKS && leftIndex % 64 != 0)
                    hidden[leftIndex] |= 0b00100000; // LEFT
                int upIndex = index - 64;
                if (upIndex > -1 && index - index / 4096 * 4096 > 64)
                    hidden[upIndex] |= 0b00001000; // UP
                int downIndex = index + 64;
                if (downIndex < DataConstants.CHUNK_MAX_BLOCKS && downIndex - downIndex / 4096 * 4096 > 64)
                    hidden[downIndex] |= 0b00000100; // DOWN
                int backIndex = index + 4096;
                if (backIndex < DataConstants.CHUNK_MAX_BLOCKS)
                    hidden[backIndex] |= 0b00000001; // BACK
                int frontIndex = index - 4096;
                if (frontIndex > -1)
                    hidden[frontIndex] |= 0b00000010; // FRONT
            }
        }

        // Reset iterator to starting position
        it.reset();

        int atlasSize = textures.getAtlasSize();

        int block = 0;
        int vertIndex = 0;
        while (!it.isDone()) {
            int begin = it.getOffset();
            short blockId = it.nextMaterial();
            if (blockId == 0) { // TODO better AIR check
                continue;
            }
            //System.out.println(blockId);

            float scale = 0.125f;
            for (int i = 0; i < it.getCount(); i++) { // Loop blocks from what we just read
                System.out.println("count: " + it.getCount());
                byte faces = hidden[begin + i]; // Read hidden faces of this block
                if (blockId == 0 || faces == 0b00111111) { // TODO better "is-air" check
                    block++; // To next block!
                    continue; // AIR or all faces are hidden
                }
                //System.out.println("id: " + id + ", block: " + block);
                TerraTexture texture = textures.getTexture(blockId); // Get texture for id
                if (texture == null) {
                    block++; // To next block!
                    continue;
                }
                
                // Calculate texture coordinates...
                int page = texture.getPage();
                int tile = texture.getTileId();

                int z = block / 4096; // Integer division: current z index
                int y = (block - 4096 * z) / 64;
                int x = block % 64;

                //System.out.println("x: " + x + ", y: " + y + ", z: " + z);

                //System.out.println("texMinX: " + texMinX + ", texMinY: " + texMinY + ", texMaxX: " + texMaxX + ", texMaxY: " + texMaxY);

                if ((faces & 0b00100000) == 0) { // RIGHT
                    //System.out.println("Draw RIGHT");
                    mesh.vertex(x, y, z + 1);
                    mesh.vertex(x, y + 1, z + 1);
                    mesh.vertex(x, y + 1, z);
                    mesh.vertex(x, y, z);
                    
                    mesh.triangle(vertIndex, 0, 1, 2);
                    mesh.triangle(vertIndex, 2, 3, 0);

                    vertIndex += 4; // Next thing is next face
                    
                    mesh.texture(page, tile, 1, 1);
                    mesh.texture(page, tile, 1, 0);
                    mesh.texture(page, tile, 0, 0);
                    mesh.texture(page, tile, 0, 1);
                } if ((faces & 0b00010000) == 0) { // LEFT
                    //System.out.println("Draw LEFT");
                    mesh.vertex(x + 1, y, z);
                    mesh.vertex(x + 1, y + 1, z);
                    mesh.vertex(x + 1, y + 1, z + 1);
                    mesh.vertex(x + 1, y, z + 1);
                    
                    mesh.triangle(vertIndex, 0, 1, 2);
                    mesh.triangle(vertIndex, 2, 3, 0);

                    vertIndex += 4; // Next thing is next face
                    
                    mesh.texture(page, tile, 0, 0);
                    mesh.texture(page, tile, 0, 1);
                    mesh.texture(page, tile, 1, 1);
                    mesh.texture(page, tile, 1, 0);
                } if ((faces & 0b00001000) == 0) { // UP
                    //System.out.println("Draw UP");
                    mesh.vertex(x, y + 1, z);
                    mesh.vertex(x, y + 1, z + 1);
                    mesh.vertex(x + 1, y + 1, z + 1);
                    mesh.vertex(x + 1, y + 1, z);
                    
                    mesh.triangle(vertIndex, 0, 1, 2);
                    mesh.triangle(vertIndex, 2, 3, 0);

                    vertIndex += 4; // Next thing is next face
                    
                    mesh.texture(page, tile, 0, 0);
                    mesh.texture(page, tile, 0, 1);
                    mesh.texture(page, tile, 1, 1);
                    mesh.texture(page, tile, 1, 0);
                } if ((faces & 0b00000100) == 0) { // DOWN
                    //System.out.println("Draw DOWN");
                    mesh.vertex(x, y, z + 1);
                    mesh.vertex(x, y, z);
                    mesh.vertex(x + 1, y, z);
                    mesh.vertex(x + 1, y, z + 1);
                    
                    mesh.triangle(vertIndex, 0, 1, 2);
                    mesh.triangle(vertIndex, 2, 3, 0);

                    vertIndex += 4; // Next thing is next face
                    
                    mesh.texture(page, tile, 0, 0);
                    mesh.texture(page, tile, 0, 1);
                    mesh.texture(page, tile, 1, 1);
                    mesh.texture(page, tile, 1, 0);
                } if ((faces & 0b00000010) == 0) { // BACK
                    //System.out.println("Draw BACK");
                    mesh.vertex(x + 1, y, z + 1);
                    mesh.vertex(x + 1, y + 1, z + 1);
                    mesh.vertex(x, y + 1, z + 1);
                    mesh.vertex(x, y, z + 1);
                    
                    mesh.triangle(vertIndex, 0, 1, 2);
                    mesh.triangle(vertIndex, 2, 3, 0);

                    vertIndex += 4; // Next thing is next face
                    
                    mesh.texture(page, tile, 0, 0);
                    mesh.texture(page, tile, 0, 1);
                    mesh.texture(page, tile, 1, 1);
                    mesh.texture(page, tile, 1, 0);
                } if ((faces & 0b00000001) == 0) { // FRONT
                    //System.out.println("Draw FRONT");
                    mesh.vertex(x, y, z);
                    mesh.vertex(x, y + 1, z);
                    mesh.vertex(x + 1, y + 1, z);
                    mesh.vertex(x + 1, y, z);
                    
                    mesh.triangle(vertIndex, 0, 1, 2);
                    mesh.triangle(vertIndex, 2, 3, 0);

                    vertIndex += 4; // Next thing is next face
                    
                    mesh.texture(page, tile, 0, 0);
                    mesh.texture(page, tile, 0, 1);
                    mesh.texture(page, tile, 1, 1);
                    mesh.texture(page, tile, 1, 0);
                }

                block++; // Go to next block
            }
        }
    }

    @Override
    public void cube(short id, float scale, TextureManager textures, MeshContainer mesh) {
        // TODO implement this
    }
}



