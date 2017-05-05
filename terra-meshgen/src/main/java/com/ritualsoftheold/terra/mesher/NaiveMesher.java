package com.ritualsoftheold.terra.mesher;

import java.util.Arrays;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.offheap.DataConstants;

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
    private FloatList texCoords;
    
    public NaiveMesher() {
       verts = new FloatArrayList();
       indices = new IntArrayList();
       texCoords = new FloatArrayList();
    }

    @Override
    public void chunk(long addr, MaterialRegistry reg) {
        // Clear previous lists
        verts.clear();
        indices.clear();
        texCoords.clear();
        
        byte[] hidden = new byte[DataConstants.CHUNK_MAX_BLOCKS]; // Visibility mappings for cubes
        //Arrays.fill(hidden, (byte) 0);
        
        // Generate mappings for culling
        for (int i = 0; i < DataConstants.CHUNK_MAX_BLOCKS; i += 4) {
            long ids = mem.readLong(addr + i * 2); // Read 4 16 bit blocks
            // BEWARE: it is LITTLE ENDIAN, not big endian as Java usually
            
            for (int j = 0; j < 4; j++) { // Loop blocks from what we just read (TODO unroll loop and measure performance)
                long id = ids >>> (j * 16); // Get id for THIS block
                if (id != 0) { // TODO better "is-air" check
                    int index = i + j;
                    
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
        }
        
        int block = 0;
        int vertIndex = 0;
        for (int i = 0; i < DataConstants.CHUNK_MAX_BLOCKS; i += 4) {
            long ids = mem.readLong(addr + i * 2); // Read 4 16 bit blocks
            // BEWARE: it is LITTLE ENDIAN, not big endian as Java usually
            if (ids == 0) {
                block += 4;
                continue;
            } else {
                //System.out.println("read: " + (addr + i * 2));
                //System.out.println("non-air: " + Long.toBinaryString(ids));
            }
            
            float scale = 0.125f;
            for (int j = 0; j < 4; j++) { // Loop blocks from what we just read (TODO unroll loop and measure performance)
                long id = ids >>> (j * 16) & 0xffff; // Get id for THIS block
                //System.out.println((48 - j * 16) + ": " + Long.toBinaryString(id));
                byte faces = hidden[i + j]; // Read hidden faces of this block
                if (id == 0 || faces == 0b00111111) { // TODO better "is-air" check
                    //System.out.println("AIR");
                    block++; // Goto next block
                    continue; // AIR or all faces are hidden
                }
                //System.out.println("id: " + id + ", block: " + block);
                
                float z0 = block / 4096;
                float z = z0 * scale * 2;
                float y = (block - 4096 * z0) / 64 * scale * 2;
                float x = block % 64 * scale * 2;
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
                    
                    // TODO implement textures
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
                }
                
                block++; // Go to next block
            }
        }
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
    public FloatList getTextureCoords() {
        return texCoords;
    }
    
    

}
