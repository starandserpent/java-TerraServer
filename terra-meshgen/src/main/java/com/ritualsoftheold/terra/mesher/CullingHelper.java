package com.ritualsoftheold.terra.mesher;

import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.core.buffer.BlockBuffer;
import com.ritualsoftheold.terra.core.material.TerraTexture;

import java.util.Objects;

/**
 * Simple face-culling implementation. Some mesh generators might be able to
 * iterate only once, culling at same time; they'll need something custom.
 */

public class CullingHelper {

    public Voxel[] cull(BlockBuffer buf, MeshContainer mesh) {
        int index = 0;
        buf.seek(0);
        Voxel[] voxels = new Voxel[DataConstants.CHUNK_MAX_BLOCKS];
        while (buf.hasNext()) {
            TerraTexture texture = buf.read().getTexture();
            if (texture == null) { // TODO better AIR check
                buf.next();
                voxels[index] = null;
                index++;
                continue;
            }

            voxels[index] = new Voxel(texture);

            index++;
            buf.next();
        }

        index = 0;
        for (Voxel voxel : voxels) {
            // Calculate current block position (normalized by shaders)
            if (voxel != null) {
                int z = index / 4096; // Integer division: current z index
                int y = (index - 4096 * z) / 64;
                int x = index % 64;

                Face face;

                // LEFT
                if (x == 0 || x > 0 && voxels[index - 1] == null) {
                    face = new Face();
                    face.setVector3f(x, y, z + 1, 0);
                    face.setVector3f(x, y + 1, z + 1, 1);
                    face.setVector3f(x, y + 1, z, 2);
                    face.setVector3f(x, y, z, 3);
                    Objects.requireNonNull(voxel).setFace(face, 0);
                }

                // RIGHT
                if (x == 63 || voxels[index + 1] == null) {
                    face = new Face();
                    face.setVector3f(x + 1, y, z, 0);
                    face.setVector3f(x + 1, y + 1, z, 1);
                    face.setVector3f(x + 1, y + 1, z + 1, 2);
                    face.setVector3f(x + 1, y, z + 1, 3);
                    Objects.requireNonNull(voxel).setFace(face, 1);
                }

                // TOP
                if (y == 63 || voxels[index + 64] == null) {
                    face = new Face();
                    face.setVector3f(x, y + 1, z, 0);
                    face.setVector3f(x, y + 1, z + 1, 1);
                    face.setVector3f(x + 1, y + 1, z + 1, 2);
                    face.setVector3f(x + 1, y + 1, z, 3);
                    Objects.requireNonNull(voxel).setFace(face, 2);
                }

                // BOTTOM
                if (y == 0 || voxels[index - 64] == null) {
                    face = new Face();
                    face.setVector3f(x + 1, y, z, 0);
                    face.setVector3f(x + 1, y, z + 1, 1);
                    face.setVector3f(x, y, z+ 1, 2);
                    face.setVector3f(x, y, z, 3);
                    Objects.requireNonNull(voxel).setFace(face, 3);
                }

                // BACK
                if (z == 63 || voxels[index + 4096] == null) {
                    face = new Face();
                    face.setVector3f(x + 1, y, z + 1, 0);
                    face.setVector3f(x + 1, y + 1, z + 1, 1);
                    face.setVector3f(x, y + 1, z + 1, 2);
                    face.setVector3f(x, y, z + 1, 3);
                    Objects.requireNonNull(voxel).setFace(face, 4);
                }

                // FRONT
                if (z == 0 || voxels[index - 4096] == null) {
                    face = new Face();
                    face.setVector3f(x, y, z, 0);
                    face.setVector3f(x, y + 1, z, 1);
                    face.setVector3f(x + 1, y + 1, z, 2);
                    face.setVector3f(x + 1, y, z, 3);
                    Objects.requireNonNull(voxel).setFace(face, 5);
                }
            }
            index++;
        }
        return voxels;
    }
}
