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
        for (int i = 0; i < DataConstants.CHUNK_MAX_BLOCKS; i += 8) {
            long ids = mem.readLong(addr + i); // Read 4 16 bit blocks
            
            for (int j = 0; j < 4; j++) { // Loop blocks from what we just read (TODO unroll loop and measure performance)
                long id = ids >>> (48 - j * 16); // Get id for THIS block
                if (id != 0) { // TODO better "is-air" check
                    int index = i + j;
                    
                    int rightIndex = index - 1;
                    if (rightIndex > -1)
                        hidden[rightIndex] |= 0b00100000; // RIGHT
                    int leftIndex = index + 1;
                    if (leftIndex < DataConstants.CHUNK_MAX_BLOCKS)
                        hidden[leftIndex] |= 0b00010000; // LEFT
                    int upIndex = index + 64;
                    if (upIndex < DataConstants.CHUNK_MAX_BLOCKS)
                        hidden[upIndex] |= 0b00001000; // UP
                    int downIndex = index + 64;
                    if (downIndex > -1)
                        hidden[downIndex] |= 0b00000100; // DOWN
                    int backIndex = index + 4096;
                    if (backIndex < DataConstants.CHUNK_MAX_BLOCKS)
                        hidden[backIndex] |= 0b00000010; // BACK
                    int frontIndex = index - 4096;
                    if (frontIndex > -1)
                        hidden[frontIndex] |= 0b00000001; // FRONT
                }
            }
        }
        
        for (int i = 0; i < DataConstants.CHUNK_MAX_BLOCKS; i += 4) {
            long ids = mem.readLong(addr + i * 2); // Read 4 16 bit blocks
            
            for (int j = 0; j < 4; j++) { // Loop blocks from what we just read (TODO unroll loop and measure performance)
                long id = ids >>> (48 - j * 16); // Get id for THIS block
                byte faces = hidden[i + j]; // Read hidden faces of this block
                if (id == 0 || faces == 0b00111111) // TODO better "is-air" check
                    continue; // AIR or all faceshidden
                System.out.println("id: " + id);
                
                float scale = 0.125f;
                if ((faces & 0b00100000) == 0) { // RIGHT
                    System.out.println("Draw RIGHT");
                    verts.add(-scale);
                    verts.add(-scale);
                    verts.add(-scale);
                    
                    verts.add(-scale);
                    verts.add(scale);
                    verts.add(-scale);
                    
                    verts.add(-scale);
                    verts.add(scale);
                    verts.add(scale);
                    
                    verts.add(-scale);
                    verts.add(-scale);
                    verts.add(scale);
                    
                    indices.add(0);
                    indices.add(1);
                    indices.add(2);
                    
                    indices.add(2);
                    indices.add(3);
                    indices.add(0);
                    
                    // TODO implement textures
                } if ((faces & 0b00010000) == 0) { // LEFT
                    System.out.println("Draw LEFT");
                    verts.add(scale);
                    verts.add(-scale);
                    verts.add(-scale);
                    
                    verts.add(scale);
                    verts.add(scale);
                    verts.add(-scale);
                    
                    verts.add(scale);
                    verts.add(scale);
                    verts.add(scale);
                    
                    verts.add(scale);
                    verts.add(-scale);
                    verts.add(scale);
                    
                    indices.add(2);
                    indices.add(1);
                    indices.add(0);
                    
                    indices.add(0);
                    indices.add(3);
                    indices.add(2);
                } if ((faces & 0b00001000) == 0) { // UP
                    System.out.println("Draw UP");
                    verts.add(-scale);
                    verts.add(scale);
                    verts.add(-scale);
                    
                    verts.add(-scale);
                    verts.add(scale);
                    verts.add(scale);
                    
                    verts.add(scale);
                    verts.add(scale);
                    verts.add(scale);
                    
                    verts.add(scale);
                    verts.add(scale);
                    verts.add(-scale);
                    
                    indices.add(2);
                    indices.add(1);
                    indices.add(0);
                    
                    indices.add(0);
                    indices.add(3);
                    indices.add(2);
                } if ((faces & 0b00000100) == 0) { // DOWN
                    System.out.println("Draw DOWN");
                    verts.add(-scale);
                    verts.add(-scale);
                    verts.add(scale);
                    
                    verts.add(-scale);
                    verts.add(-scale);
                    verts.add(-scale);
                    
                    verts.add(scale);
                    verts.add(-scale);
                    verts.add(-scale);
                    
                    verts.add(scale);
                    verts.add(-scale);
                    verts.add(scale);
                    
                    indices.add(2);
                    indices.add(1);
                    indices.add(0);
                    
                    indices.add(0);
                    indices.add(3);
                    indices.add(2);
                } if ((faces & 0b00000010) == 0) { // BACK
                    System.out.println("Draw BACK");
                    verts.add(scale);
                    verts.add(-scale);
                    verts.add(scale);
                    
                    verts.add(scale);
                    verts.add(scale);
                    verts.add(scale);
                    
                    verts.add(-scale);
                    verts.add(scale);
                    verts.add(scale);
                    
                    verts.add(-scale);
                    verts.add(-scale);
                    verts.add(scale);
                    
                    indices.add(2);
                    indices.add(1);
                    indices.add(0);
                    
                    indices.add(0);
                    indices.add(3);
                    indices.add(2);
                } if ((faces & 0b00000001) == 0) { // FRONT
                    System.out.println("Draw FRONT");
                    verts.add(-scale);
                    verts.add(-scale);
                    verts.add(-scale);
                    
                    verts.add(-scale);
                    verts.add(scale);
                    verts.add(-scale);
                    
                    verts.add(scale);
                    verts.add(scale);
                    verts.add(-scale);
                    
                    verts.add(scale);
                    verts.add(-scale);
                    verts.add(-scale);
                    
                    indices.add(2);
                    indices.add(1);
                    indices.add(0);
                    
                    indices.add(0);
                    indices.add(3);
                    indices.add(2);
                }
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
