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
 */
public class NaiveMesher implements VoxelMesher {
    
    private static final Memory mem = OS.memory();

    @Override
    public long chunk(long addr, MaterialRegistry reg) {
        byte[] hidden = new byte[DataConstants.CHUNK_MAX_BLOCKS]; // Visibility mappings for cubes
        Arrays.fill(hidden, (byte) 0);
        
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
        
        // Initialize mesh data arrays (TODO optimize this to create less garbage)
        FloatList verts = new FloatArrayList();
        IntList indices = new IntArrayList();
        FloatList texCoords = new FloatArrayList();
        
        for (int i = 0; i < DataConstants.CHUNK_MAX_BLOCKS; i += 4) {
            long ids = mem.readLong(addr + i * 2); // Read 4 16 bit blocks
            
            for (int j = 0; j < 4; j++) { // Loop blocks from what we just read (TODO unroll loop and measure performance)
                long id = ids >>> (48 - j * 16); // Get id for THIS block
                byte faces = hidden[i + j]; // Read hidden faces of this block
                if (id == 0 || faces == 0) // TODO better "is-air" check
                    continue; // AIR or all faceshidden
                
                float scale = 0.125f;
                if ((faces & 0b00100000) != 0) { // RIGHT
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
                }
            }
        }
        
        return 0; // TODO
    }

    @Override
    public long octree(long addr, MaterialRegistry reg) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void release(long addr) {
        // TODO Auto-generated method stub
        
    }

}
