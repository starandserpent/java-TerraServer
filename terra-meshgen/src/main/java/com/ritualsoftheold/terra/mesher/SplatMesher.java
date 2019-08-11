package com.ritualsoftheold.terra.mesher;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.ritualsoftheold.terra.offheap.chunk.ChunkLArray;

import java.util.ArrayList;

public class SplatMesher {

    public void chunk(ChunkLArray chunk, ArrayList<Vector3f> vector3fs, ArrayList<ColorRGBA> colors) {

        for (int index = 0; index < ChunkLArray.CHUNK_SIZE; index++) {
            //Position of current voxel
            int z = index / 4096;
            int y = (index - 4096 * z) / 64;
            int x = index % 64;

            boolean canCreateVoxel = false;

            //Left
            if (x == 0 || chunk.get(index - 1).getTexture() != null) {
                canCreateVoxel = true;
            }

            //Right
            else if (x == 63 || chunk.get(index + 1) != null) {
                canCreateVoxel = true;
            }
            //Top
            else if (y == 63 || chunk.get(index + 64) != null) {
                canCreateVoxel = true;
            }
            //Bottom
            else if (y == 0 || chunk.get(index - 64) != null) {
                canCreateVoxel = true;
            }
            //Back
            else if (z == 63 || chunk.get(index + 4096) != null) {
                canCreateVoxel = true;
            }
            //Front
            else if (z == 0 || chunk.get(index - 4096) != null) {
                canCreateVoxel = true;
            }

            if (canCreateVoxel) {
                vector3fs.add(new Vector3f(x, y, z));
                colors.add(ColorRGBA.randomColor());
            }
        }
    }
}
