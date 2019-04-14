package com.ritualsoftheold.terra.mesher;

import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.core.buffer.BlockBuffer;
import com.ritualsoftheold.terra.core.material.TerraTexture;

/**
 * Simple face-culling implementation. Some mesh generators might be able to
 * iterate only once, culling at same time; they'll need something custom.
 *
 */
public class CullingHelper {

    public void cull(BlockBuffer buf, byte[] hidden) {
        int index = 0;
        buf.seek(0);
        while (buf.hasNext()) {
            TerraTexture texture = buf.read().getTexture();
            if (texture == null) { // TODO better AIR check
                continue;
            }

            /*
             * Following code is performance critical according to JMH
             * throughput testing. So, we do some optimizations that
             * would normally be quite useless:
             * 
             * index % 2^n == index & (2^n - 1)
             * 
             * So basically we replace modulo with a bitwise AND.
             * This increases total mesher performance by about 25%.
             */
            int rightIndex = index - 1;
            if (rightIndex > -1 && (index & 63) != 0)
                hidden[rightIndex] |= 0b00010000; // RIGHT
            int leftIndex = index + 1;
            if (leftIndex < DataConstants.CHUNK_MAX_BLOCKS && (leftIndex & 63) != 0)
                hidden[leftIndex] |= 0b00100000; // LEFT
            int upIndex = index - 64;
            if (upIndex > -1 && index - index / 4096 * 4096 > 63)
                hidden[upIndex] |= 0b00001000; // UP
            int downIndex = index + 64;
            if (downIndex < DataConstants.CHUNK_MAX_BLOCKS && downIndex - downIndex / 4096 * 4096 > 63)
                hidden[downIndex] |= 0b00000100; // DOWN
            int backIndex = index + 4096;
            if (backIndex < DataConstants.CHUNK_MAX_BLOCKS)
                hidden[backIndex] |= 0b00000001; // BACK
            int frontIndex = index - 4096;
            if (frontIndex > -1)
                hidden[frontIndex] |= 0b00000010; // FRONT

            buf.next();
            index++;
        }
    }
}
