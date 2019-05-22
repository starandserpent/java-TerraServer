package com.ritualsoftheold.terra.mesher;

import com.ritualsoftheold.terra.core.buffer.BlockBuffer;
import com.ritualsoftheold.terra.mesher.resource.MeshContainer;
import com.ritualsoftheold.terra.mesher.resource.TextureManager;

import java.util.*;

/**
 * Greedy mesher does culling and try to merge same blocks into bigger faces.
 */
public class GreedyMesher implements VoxelMesher {
    public GreedyMesher(){}

    @Override
    public void chunk(BlockBuffer buf, TextureManager textures, MeshContainer mesh) {
        assert buf != null;
        assert textures != null;
        assert mesh != null;

        NaiveGreedyMesher culling = new NaiveGreedyMesher();

        // Generate mappings for culling
        HashMap<Integer, HashMap<Integer, Face>> sector = culling.cull(buf);

        // Reset buffer to starting position
        buf.seek(0);
        int verticeIndex = 0;

        for(Integer key:sector.keySet()) {
            HashMap<Integer, Face> faces = sector.get(key);
            Integer[] keys = new Integer[faces.keySet().size()];
            faces.keySet().toArray(keys);
            Arrays.sort(keys);
            for (int i = keys.length - 1; i >= 0; i--) {
                int index = keys[i];
                joinReversed(faces, index, key);
            }

            setTextureCoords(faces.values(), key);
            verticeIndex = fillContainer(mesh, faces.values(), verticeIndex);
            faces.clear();
        }

        sector.clear();
    }

    //Setting textures for mesh
    private static void setTextureCoords(Collection<Face> faces, int side) {
        for (Face completeFace : faces) {
            switch (side) {
                case 0:
                case 1:
                    completeFace.setTextureCoords(completeFace.getVector3fs()[0].z*4, completeFace.getVector3fs()[0].y*4, 0);
                    completeFace.setTextureCoords(completeFace.getVector3fs()[0].z*4,  completeFace.getVector3fs()[2].y*4,1);
                    completeFace.setTextureCoords( completeFace.getVector3fs()[2].z*4,  completeFace.getVector3fs()[2].y*4, 2);
                    completeFace.setTextureCoords( completeFace.getVector3fs()[2].z*4, completeFace.getVector3fs()[0].y*4, 3);
                    break;

                case 2:
                case 3:
                    completeFace.setTextureCoords(completeFace.getVector3fs()[0].x*4, completeFace.getVector3fs()[0].z*4, 0);
                    completeFace.setTextureCoords(completeFace.getVector3fs()[0].x*4, completeFace.getVector3fs()[2].z*4,1);
                    completeFace.setTextureCoords(completeFace.getVector3fs()[2].x*4, completeFace.getVector3fs()[2].z*4, 2);
                    completeFace.setTextureCoords(completeFace.getVector3fs()[2].x*4, completeFace.getVector3fs()[0].z*4, 3);
                    break;

                case 4:
                case 5:
                    completeFace.setTextureCoords(completeFace.getVector3fs()[0].x*4, completeFace.getVector3fs()[0].y*4, 0);
                    completeFace.setTextureCoords(completeFace.getVector3fs()[0].x*4,  completeFace.getVector3fs()[2].y*4,1);
                    completeFace.setTextureCoords( completeFace.getVector3fs()[2].x*4,  completeFace.getVector3fs()[2].y*4, 2);
                    completeFace.setTextureCoords( completeFace.getVector3fs()[2].x*4, completeFace.getVector3fs()[0].y*4, 3);
                    break;
            }
        }
    }

    //Moving all values to MeshContainer
    private static int fillContainer(MeshContainer mesh, Collection<Face> faces, int verticeIndex) {
        for (Face completeFace : faces) {
            mesh.vertex(completeFace.getVector3fs());
            mesh.triangle(getIndexes(verticeIndex));
            mesh.texture(completeFace.getTextureCoords());
            mesh.normals(completeFace.getNormals());
            verticeIndex += 4;
        }

        return verticeIndex;
    }

    private static void joinReversed(HashMap<Integer, Face> faces, int index, int side) {
    int neighbor = 64;
        switch (side) {
            case 2:
            case 3:
                neighbor = 4096;
                break;
        }

        Face nextFace = faces.get(index - neighbor);
        if (nextFace == null) {
            return;
        }

        Face face = faces.get(index);
        if (face.getMaterial() == nextFace.getMaterial()) {
            if (nextFace.getVector3fs()[2].equals(face.getVector3fs()[3]) && nextFace.getVector3fs()[1].equals(face.getVector3fs()[0])) {
                nextFace.setVector3f(face.getVector3fs()[1], 1);
                nextFace.setVector3f(face.getVector3fs()[2], 2);
                faces.remove(index);
            } else if (nextFace.getVector3fs()[3].equals(face.getVector3fs()[2]) && nextFace.getVector3fs()[0].equals(face.getVector3fs()[1])) {
                nextFace.setVector3f(face.getVector3fs()[3], 3);
                nextFace.setVector3f(face.getVector3fs()[0], 0);
                faces.remove(index);
            }
        }
    }

    private static int[] getIndexes(int verticeIndex) {
        int[] indexes = new int[6];
        indexes[0] = verticeIndex;
        indexes[1] = verticeIndex + 2;
        indexes[2] = verticeIndex + 3;
        indexes[3] = verticeIndex + 2;
        indexes[4] = verticeIndex;
        indexes[5] = verticeIndex + 1;
        return indexes;
    }

    @Override
    public void cube(int id, float scale, TextureManager textures, MeshContainer mesh) {

    }
}