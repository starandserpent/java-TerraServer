package com.ritualsoftheold.terra.mesher;

import com.ritualsoftheold.terra.mesher.resource.TextureManager;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.core.material.TerraMaterial;
import com.ritualsoftheold.terra.core.material.TerraTexture;
import com.ritualsoftheold.terra.offheap.data.BufferWithFormat;

/**
 * Naive mesher does culling, but doesn't try to merge same blocks into bigger faces.
 *
 */
public class NaiveMesher implements VoxelMesher {

    private CullingHelper culling;
    
    public NaiveMesher(CullingHelper culling) {
        this.culling = culling;
    }
    
    public NaiveMesher() {
        this(new CullingHelper());
    }
    
    @Override
    public void chunk(BufferWithFormat buf, TextureManager textures, MeshContainer mesh) {
        assert buf != null;
        assert textures != null;
        assert mesh != null;

        byte[] hidden = new byte[DataConstants.CHUNK_MAX_BLOCKS]; // Visibility mappings for cubes
        //Arrays.fill(hidden, (byte) 0);

        // Generate mappings for culling
        culling.cull(buf, hidden);

        // Reset buffer to starting position
        buf.seek(0);

        int atlasSize = textures.getAtlasSize();

        int block = 0;
        int vertIndex = 0;
        while (buf.hasNext()) {
            TerraMaterial material = buf.read();
            buf.next();
            if (material.getWorldId() == 1 || material.getTexture() == null) { // TODO better AIR check
                block++;
                continue;
            }
            //System.out.println(blockId);

            //System.out.println("count: " + it.getCount());
            byte faces = hidden[block]; // Read hidden faces of this block
            if (faces == 0b00111111) { // TODO better "is-air" check
                block++; // To next block!
                continue; // AIR or all faces are hidden
            }
            //System.out.println("id: " + id + ", block: " + block);

            TerraTexture texture = material.getTexture();
            // Calculate texture coordinates...
            int page = texture.getPage();
            int tile = texture.getTileId();
            float texScale = texture.getScale();
            int perSide = texture.getTexturesPerSide();
            mesh.addTexture(texture);

            // Calculate current block position (normalized by shader)
            int z = block / 4096; // Integer division: current z index
            int y = (block - 4096 * z) / 64;
            int x = block % 64;

            //System.out.println("x: " + x + ", y: " + y + ", z: " + z);

            //System.out.println("texMinX: " + texMinX + ", texMinY: " + texMinY + ", texMaxX: " + texMaxX + ", texMaxY: " + texMaxY);

            if ((faces & 0b00100000) == 0) { // LEFT
                //System.out.println("Draw LEFT");
                mesh.vertex(x, y, z + 1);
                mesh.vertex(x, y + 1, z + 1);
                mesh.vertex(x, y + 1, z);
                mesh.vertex(x, y, z);
                
                mesh.triangle(vertIndex, 0, 1, 2);
                mesh.triangle(vertIndex, 2, 3, 0);

                vertIndex += 4; // Next thing is next face
                
                mesh.texture(page, tile, perSide, texScale * (63 - z), texScale * y);
                mesh.texture(page, tile, perSide, texScale * (63 - z), texScale * (y + 1));
                mesh.texture(page, tile, perSide, texScale * (64 - z), texScale * (y + 1));
                mesh.texture(page, tile, perSide, texScale * (64 - z), texScale * y);
            } if ((faces & 0b00010000) ==  0) { // RIGHT
                //System.out.println("Draw RIGHT");
                mesh.vertex(x + 1, y, z);
                mesh.vertex(x + 1, y + 1, z);
                mesh.vertex(x + 1, y + 1, z + 1);
                mesh.vertex(x + 1, y, z + 1);
                
                mesh.triangle(vertIndex, 0, 1, 2);
                mesh.triangle(vertIndex, 2, 3, 0);

                vertIndex += 4; // Next thing is next face
                
                mesh.texture(page, tile, perSide, texScale * z, texScale * y);
                mesh.texture(page, tile, perSide, texScale * z, texScale * (y + 1));
                mesh.texture(page, tile, perSide, texScale * (z + 1), texScale * (y + 1));
                mesh.texture(page, tile, perSide, texScale * (z + 1), texScale * y);
            } if ((faces & 0b00001000) == 0) { // UP
                //System.out.println("Draw UP");
                mesh.vertex(x, y + 1, z);
                mesh.vertex(x, y + 1, z + 1);
                mesh.vertex(x + 1, y + 1, z + 1);
                mesh.vertex(x + 1, y + 1, z);
                
                mesh.triangle(vertIndex, 0, 1, 2);
                mesh.triangle(vertIndex, 2, 3, 0);

                vertIndex += 4; // Next thing is next face
                
                mesh.texture(page, tile, perSide, texScale * (64 - x), texScale * (64 - z));
                mesh.texture(page, tile, perSide, texScale * (64 - x), texScale * (63 - z));
                mesh.texture(page, tile, perSide, texScale * (63 - x), texScale * (63 - z));
                mesh.texture(page, tile, perSide, texScale * (63 - x), texScale * (64 - z));
            } if ((faces & 0b00000100) == 0) { // DOWN
                //System.out.println("Draw DOWN");
                mesh.vertex(x, y, z + 1);
                mesh.vertex(x, y, z);
                mesh.vertex(x + 1, y, z);
                mesh.vertex(x + 1, y, z + 1);
                
                mesh.triangle(vertIndex, 0, 1, 2);
                mesh.triangle(vertIndex, 2, 3, 0);

                vertIndex += 4; // Next thing is next face
                
                mesh.texture(page, tile, perSide, texScale * x, texScale * (63 - z));
                mesh.texture(page, tile, perSide, texScale * x, texScale * (64 - z));
                mesh.texture(page, tile, perSide, texScale * (x + 1), texScale * (64 - z));
                mesh.texture(page, tile, perSide, texScale * (x + 1), texScale * (63 - z));
            } if ((faces & 0b00000010) == 0) { // BACK
                //System.out.println("Draw BACK");
                mesh.vertex(x + 1, y, z + 1);
                mesh.vertex(x + 1, y + 1, z + 1);
                mesh.vertex(x, y + 1, z + 1);
                mesh.vertex(x, y, z + 1);
                
                mesh.triangle(vertIndex, 0, 1, 2);
                mesh.triangle(vertIndex, 2, 3, 0);

                vertIndex += 4; // Next thing is next face
                
                mesh.texture(page, tile, perSide, texScale * (63 - x), texScale * y);
                mesh.texture(page, tile, perSide, texScale * (63 - x), texScale * (y + 1));
                mesh.texture(page, tile, perSide, texScale * (64 - x), texScale * (y + 1));
                mesh.texture(page, tile, perSide, texScale * (64 - x), texScale * y);
            } if ((faces & 0b00000001) == 0) { // FRONT
                //System.out.println("Draw FRONT");
                mesh.vertex(x, y, z);
                mesh.vertex(x, y + 1, z);
                mesh.vertex(x + 1, y + 1, z);
                mesh.vertex(x + 1, y, z);
                
                mesh.triangle(vertIndex, 0, 1, 2);
                mesh.triangle(vertIndex, 2, 3, 0);

                vertIndex += 4; // Next thing is next face
                
                mesh.texture(page, tile, perSide, texScale * x, texScale * y);
                mesh.texture(page, tile, perSide, texScale * x, texScale * (y + 1));
                mesh.texture(page, tile, perSide, texScale * (x + 1), texScale * (y + 1));
                mesh.texture(page, tile, perSide, texScale * (x + 1), texScale * y);
            }

            block++; // Go to next block
        }        
    }

    @Override
    public void cube(int id, float scale, TextureManager textures, MeshContainer mesh) {
        // TODO implement this
    }
}



