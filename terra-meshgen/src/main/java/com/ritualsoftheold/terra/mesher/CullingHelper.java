package com.ritualsoftheold.terra.mesher;

import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.core.buffer.BlockBuffer;
import com.ritualsoftheold.terra.core.material.TerraTexture;

/**
 * Simple face-culling implementation. Some mesh generators might be able to
 * iterate only once, culling at same time; they'll need something custom.
 */

public class CullingHelper {

    public Face[][] cull(BlockBuffer buf) {
        int index = 0;
        buf.seek(0);
        Face[][] faces = new Face[DataConstants.CHUNK_MAX_BLOCKS][];
        while (buf.hasNext()) {
            TerraTexture texture = buf.read().getTexture();
            if (texture == null) { // TODO better AIR check
                buf.next();
                faces[index] = null;
                index++;
                continue;
            }

            faces[index] = new Face[6];
            index++;
            buf.next();
        }

        index = 0;
        for (Face[] voxel : faces) {
            // Calculate current block position (normalized by shaders)
            if (voxel != null) {
                int z = index / 4096; // Integer division: current z index
                int y = (index - 4096 * z) / 64;
                int x = index % 64;

                Face face;

                // LEFT
                if (x == 0 || x > 0 && faces[index - 1] == null) {
                    face = new Face();
                    face.setVector3f(x, y, z + 1, 0);
                    face.setVector3f(x, y + 1, z + 1, 1);
                    face.setVector3f(x, y + 1, z, 2);
                    face.setVector3f(x, y, z, 3);
                    voxel[0] = face;
                }

                // RIGHT
                if (x == 63 || faces[index + 1] == null) {
                    face = new Face();
                    face.setVector3f(x + 1, y, z, 0);
                    face.setVector3f(x + 1, y + 1, z, 1);
                    face.setVector3f(x + 1, y + 1, z + 1, 2);
                    face.setVector3f(x + 1, y, z + 1, 3);
                    voxel[1] = face;
                }

                // TOP
                if (y == 63 || faces[index + 64] == null) {
                    face = new Face();
                    face.setVector3f(x, y + 1, z, 0);
                    face.setVector3f(x, y + 1, z + 1, 1);
                    face.setVector3f(x + 1, y + 1, z + 1, 2);
                    face.setVector3f(x + 1, y + 1, z, 3);
                    voxel[2] = face;
                }

                // BOTTOM
                if (y == 0 || faces[index - 64] == null) {
                    face = new Face();
                    face.setVector3f(x + 1, y, z, 0);
                    face.setVector3f(x + 1, y, z + 1, 1);
                    face.setVector3f(x, y, z+ 1, 2);
                    face.setVector3f(x, y, z, 3);
                    voxel[3] = face;
                }

                // BACK
                if (z == 63 || faces[index + 4096] == null) {
                    face = new Face();
                    face.setVector3f(x + 1, y, z + 1, 0);
                    face.setVector3f(x + 1, y + 1, z + 1, 1);
                    face.setVector3f(x, y + 1, z + 1, 2);
                    face.setVector3f(x, y, z + 1, 3);
                    voxel[4] = face;
                }
                // FRONT
                if (z == 0 || faces[index - 4096] == null) {
                    face = new Face();
                    face.setVector3f(x, y, z, 0);
                    face.setVector3f(x, y + 1, z, 1);
                    face.setVector3f(x + 1, y + 1, z, 2);
                    face.setVector3f(x + 1, y, z, 3);
                    voxel[5] = face;
                }
            }
            index++;
        }
        return faces;
    }
}
