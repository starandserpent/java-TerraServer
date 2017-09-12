package com.ritualsoftheold.terra.mesher;

import java.util.Arrays;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.material.TerraTexture;
import com.ritualsoftheold.terra.mesher.resource.TextureManager;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.iterator.ChunkIterator;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Naive mesher does culling, but doesn't try to merge same blocks into bigger faces.
 * 
 * Note: doesn't support multithreading. Please use one mesher per thread.
 *
 */
public class NaiveMesher implements VoxelMesher {
    
    private static final Memory mem = OS.memory();
    
    private FloatList verts;
    private IntList indices;
    private IntList texCoords;
    
    public NaiveMesher() {
       verts = new FloatArrayList();
       indices = new IntArrayList();
       texCoords = new IntArrayList();
    }

    @Override
    public void chunk(long addr, TextureManager textures) {
        assert addr != 0;
        assert textures != null;
        
        // Clear previous lists
        verts.clear();
        indices.clear();
        texCoords.clear();
        
        byte[] hidden = new byte[DataConstants.CHUNK_MAX_BLOCKS]; // Visibility mappings for cubes
        //Arrays.fill(hidden, (byte) 0);
        
        // Create iterator
        ChunkIterator it = ChunkIterator.forChunk(addr + DataConstants.CHUNK_DATA_OFFSET, mem.readByte(addr));
        
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
        
        float atlasSize = textures.getAtlasSize();
        
        int block = 0;
        int vertIndex = 0;
        while (!it.isDone()) {
            int begin = it.getOffset();
            short blockId = it.nextMaterial();
            int count = it.getCount();
            
            if (blockId == 0) {
                block += count;
                continue;
            }
            
            TerraTexture texture = textures.getTexture(blockId); // Get texture for id
            if (texture == null) {
                block += count;
                continue;
            }
            
            // Calculate texture coordinates...
            int texMinX = texture.getTexCoordX();
            int texMinY = texture.getTexCoordY() ;
            int texMaxX = texMinX + texture.getWidth() / texture.getScaleDivider();
            int texMaxY = texMinY + texture.getHeight() / texture.getScaleDivider();
            int texArray = texture.getTexCoordZ();
            System.out.println("texMinX: " + texMinX + ", texMinY: " + texMinY + ", texMaxX: " + texMaxX + ", texMaxY: " + texMaxY);
            
            //System.out.println(blockId);
            
            float scale = 0.125f;
            for (int i = 0; i < count; i++) { // Loop blocks from what we just read
                //System.out.println((48 - j * 16) + ": " + Long.toBinaryString(id));
                byte faces = hidden[begin + i]; // Read hidden faces of this block
                if (faces == 0b00111111) { // TODO better "is-air" check
                    block++; // To next block!
                    continue; // AIR or all faces are hidden
                }
                //System.out.println("id: " + id + ", block: " + block);
                
                float z0 = block / 4096;
                float z = z0 * scale * 2;
                float y = (block - 4096 * z0) / 64 * scale * 2;
                float x = block % 64 * scale * 2;
                
                // Reduce coordinates to have chunk center be where is needs to be
                x -= 8;
                y -= 8;
                z -= 8;
                
                //System.out.println("x: " + x + ", y: " + y + ", z: " + z);
                
                if ((faces & 0b00100000) == 0) { // RIGHT
                    //System.out.println("Draw RIGHT");
                    verts.add(x - scale);
                    verts.add(y + scale);
                    verts.add(z + scale);
                    
                    verts.add(x - scale);
                    verts.add(y + scale);
                    verts.add(z - scale);
                    
                    verts.add(x - scale);
                    verts.add(y - scale);
                    verts.add(z - scale);
                    
                    verts.add(x - scale);
                    verts.add(y - scale);
                    verts.add(z + scale);
                    
                    indices.add(vertIndex + 0);
                    indices.add(vertIndex + 1);
                    indices.add(vertIndex + 2);
                    
                    indices.add(vertIndex + 2);
                    indices.add(vertIndex + 3);
                    indices.add(vertIndex + 0);
                    
                    vertIndex += 4;
                    
                    texCoords(texMaxX, texMaxY, texArray);

                    texCoords(texMinX, texMaxY, texArray);
                    
                    texCoords(texMinX, texMinY, texArray);
                    
                    texCoords(texMaxX, texMinY, texArray);
                } if ((faces & 0b00010000) == 0) { // LEFT
                    //System.out.println("Draw LEFT");
                    verts.add(x + scale);
                    verts.add(y - scale);
                    verts.add(z - scale);
                    
                    verts.add(x + scale);
                    verts.add(y + scale);
                    verts.add(z - scale);
                    
                    verts.add(x + scale);
                    verts.add(y + scale);
                    verts.add(z + scale);
                    
                    verts.add(x + scale);
                    verts.add(y - scale);
                    verts.add(z + scale);
                    
                    indices.add(vertIndex + 0);
                    indices.add(vertIndex + 1);
                    indices.add(vertIndex + 2);
                    
                    indices.add(vertIndex + 2);
                    indices.add(vertIndex + 3);
                    indices.add(vertIndex + 0);
                    
                    vertIndex += 4;
                    
                    texCoords(texMinX, texMinY, texArray);
                    
                    texCoords(texMinX, texMaxY, texArray);
                    
                    texCoords(texMaxX, texMaxY, texArray);

                    texCoords(texMaxX, texMinY, texArray);
                } if ((faces & 0b00001000) == 0) { // UP
                    //System.out.println("Draw UP");
                    verts.add(x - scale);
                    verts.add(y + scale);
                    verts.add(z - scale);
                    
                    verts.add(x - scale);
                    verts.add(y + scale);
                    verts.add(z + scale);
                    
                    verts.add(x + scale);
                    verts.add(y + scale);
                    verts.add(z + scale);
                    
                    verts.add(x + scale);
                    verts.add(y + scale);
                    verts.add(z - scale);
                    
                    indices.add(vertIndex + 0);
                    indices.add(vertIndex + 1);
                    indices.add(vertIndex + 2);
                    
                    indices.add(vertIndex + 2);
                    indices.add(vertIndex + 3);
                    indices.add(vertIndex + 0);
                    
                    vertIndex += 4;
                    
                    texCoords(texMinX, texMinY, texArray);
                    
                    texCoords(texMinX, texMaxY, texArray);
                    
                    texCoords(texMaxX, texMaxY, texArray);

                    texCoords(texMaxX, texMinY, texArray);
                } if ((faces & 0b00000100) == 0) { // DOWN
                    //System.out.println("Draw DOWN");
                    verts.add(x - scale);
                    verts.add(y - scale);
                    verts.add(z + scale);
                    
                    verts.add(x - scale);
                    verts.add(y - scale);
                    verts.add(z - scale);
                    
                    verts.add(x + scale);
                    verts.add(y - scale);
                    verts.add(z - scale);
                    
                    verts.add(x + scale);
                    verts.add(y - scale);
                    verts.add(z + scale);
                    
                    indices.add(vertIndex + 0);
                    indices.add(vertIndex + 1);
                    indices.add(vertIndex + 2);
                    
                    indices.add(vertIndex + 2);
                    indices.add(vertIndex + 3);
                    indices.add(vertIndex + 0);
                    
                    vertIndex += 4;
                    
                    texCoords(texMinX, texMinY, texArray);
                    
                    texCoords(texMinX, texMaxY, texArray);
                    
                    texCoords(texMaxX, texMaxY, texArray);

                    texCoords(texMaxX, texMinY, texArray);
                } if ((faces & 0b00000010) == 0) { // BACK
                    //System.out.println("Draw BACK");
                    verts.add(x + scale);
                    verts.add(y - scale);
                    verts.add(z + scale);
                    
                    verts.add(x + scale);
                    verts.add(y + scale);
                    verts.add(z + scale);
                    
                    verts.add(x - scale);
                    verts.add(y + scale);
                    verts.add(z + scale);
                    
                    verts.add(x - scale);
                    verts.add(y - scale);
                    verts.add(z + scale);
                    
                    indices.add(vertIndex + 0);
                    indices.add(vertIndex + 1);
                    indices.add(vertIndex + 2);
                    
                    indices.add(vertIndex + 2);
                    indices.add(vertIndex + 3);
                    indices.add(vertIndex + 0);
                    
                    vertIndex += 4;
                    
                    texCoords(texMinX, texMinY, texArray);
                    
                    texCoords(texMinX, texMaxY, texArray);
                    
                    texCoords(texMaxX, texMaxY, texArray);

                    texCoords(texMaxX, texMinY, texArray);
                } if ((faces & 0b00000001) == 0) { // FRONT
                    //System.out.println("Draw FRONT");
                    verts.add(x - scale);
                    verts.add(y - scale);
                    verts.add(z - scale);
                    
                    verts.add(x - scale);
                    verts.add(y + scale);
                    verts.add(z - scale);
                    
                    verts.add(x + scale);
                    verts.add(y + scale);
                    verts.add(z - scale);
                    
                    verts.add(x + scale);
                    verts.add(y - scale);
                    verts.add(z - scale);
                    
                    indices.add(vertIndex + 0);
                    indices.add(vertIndex + 1);
                    indices.add(vertIndex + 2);
                    
                    indices.add(vertIndex + 2);
                    indices.add(vertIndex + 3);
                    indices.add(vertIndex + 0);
                    
                    vertIndex += 4;
                    
                    texCoords(texMinX, texMinY, texArray);
                    
                    texCoords(texMinX, texMaxY, texArray);
                    
                    texCoords(texMaxX, texMaxY, texArray);

                    texCoords(texMaxX, texMinY, texArray);
                }
                
                block++; // Go to next block
            }
        }
    }
    
    private void texCoords(int x, int y, int z) {
        x /= 2;
        y /= 2;
        
        System.out.println("texX: " + x + ", texY: " + y);
        texCoords.add(z << 11 | y << 11 | x);
        
        int inTexCoord = texCoords.getInt(texCoords.size() - 1);
        System.out.println((inTexCoord & 0x7ff) * 2 / 2048f);
        System.out.println((inTexCoord >>> 11 & 0x7ff) / 2048f);
        System.out.println(inTexCoord >>> 22);
    }

    @Override
    public void octree(long addr, MaterialRegistry reg) {
        // TODO octree meshing (simple to do, probably)
    }

    @Override
    public FloatList getVertices() {
        return verts;
    }

    @Override
    public IntList getIndices() {
        return indices;
    }

    @Override
    public IntList getTextureCoords() {
        return texCoords;
    }

}
