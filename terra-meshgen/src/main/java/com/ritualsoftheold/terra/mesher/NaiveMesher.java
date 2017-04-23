package com.ritualsoftheold.terra.mesher;

import java.util.Arrays;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.offheap.DataConstants;

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
        byte[] visible = new byte[DataConstants.CHUNK_MAX_BLOCKS]; // Visibility mappings for cubes
        Arrays.fill(visible, (byte) 0b00111111);
        
        for (int i = 0; i < DataConstants.CHUNK_MAX_BLOCKS; i += 8) {
            long ids = mem.readLong(addr + i); // Read 4 16 bit blocks
            
            for (int j = 0; j < 4; j++) { // Loop blocks from what we just read (TODO unroll loop and measure performance)
                long id = ids >>> (48 - j * 16); // Get id for THIS block
                if (id != 0) { // TODO better "is-air" check
                    int index = i + j;
                    visible[index - 1] |= 0b00100000; // RIGHT
                    visible[index + 1] |= 0b00010000; // LEFT
                    visible[index + 64] |= 0b00001000; // UP
                    visible[index - 64] |= 0b00000100; // DOWN
                    visible[index + 4096] |= 0b00000010; // BACK
                    visible[index - 4096] |= 0b00000001; // FRONT
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
