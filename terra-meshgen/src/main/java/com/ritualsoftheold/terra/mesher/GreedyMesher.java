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
    public HashMap<Integer, HashMap<Integer, Face>> chunk(BlockBuffer buf) {
        assert buf != null;

        NaiveGreedyMesher culling = new NaiveGreedyMesher();

        // Generate mappings for culling
       return  culling.cull(buf);
    }

    public void setTextureCoords(Collection<Face> faces, int side) {
        for (Face completeFace : faces) {
            switch (side) {
                case 0:
                case 1:
                    completeFace.setTextureCoords(completeFace.getVector3fs()[0].z*2048f/completeFace.getMaterial().getTexture().getWidth(),completeFace.getVector3fs()[0].y*2048f/completeFace.getMaterial().getTexture().getHeight(),0);
                    completeFace.setTextureCoords(completeFace.getVector3fs()[0].z*2048f/completeFace.getMaterial().getTexture().getWidth(),completeFace.getVector3fs()[2].y*2048f/completeFace.getMaterial().getTexture().getHeight(),1);
                    completeFace.setTextureCoords(completeFace.getVector3fs()[2].z*2048f/completeFace.getMaterial().getTexture().getWidth(),completeFace.getVector3fs()[2].y*2048f/completeFace.getMaterial().getTexture().getHeight(),2);
                    completeFace.setTextureCoords(completeFace.getVector3fs()[2].z*2048f/completeFace.getMaterial().getTexture().getWidth(),completeFace.getVector3fs()[0].y*2048f/completeFace.getMaterial().getTexture().getHeight(),3);
                    break;

                case 2:
                case 3:
                    completeFace.setTextureCoords(completeFace.getVector3fs()[0].x*2048f/completeFace.getMaterial().getTexture().getWidth(), completeFace.getVector3fs()[0].z*2048f/completeFace.getMaterial().getTexture().getHeight(),0);
                    completeFace.setTextureCoords(completeFace.getVector3fs()[0].x*2048f/completeFace.getMaterial().getTexture().getWidth(), completeFace.getVector3fs()[2].z*2048f/completeFace.getMaterial().getTexture().getHeight(),1);
                    completeFace.setTextureCoords(completeFace.getVector3fs()[2].x*2048f/completeFace.getMaterial().getTexture().getWidth(), completeFace.getVector3fs()[2].z*2048f/completeFace.getMaterial().getTexture().getHeight(),2);
                    completeFace.setTextureCoords(completeFace.getVector3fs()[2].x*2048f/completeFace.getMaterial().getTexture().getWidth(), completeFace.getVector3fs()[0].z*2048f/completeFace.getMaterial().getTexture().getHeight(),3);
                    break;

                case 4:
                case 5:
                    completeFace.setTextureCoords(completeFace.getVector3fs()[0].x*2048f/completeFace.getMaterial().getTexture().getWidth(),completeFace.getVector3fs()[0].y*2048f/completeFace.getMaterial().getTexture().getWidth(), 0);
                    completeFace.setTextureCoords(completeFace.getVector3fs()[0].x*2048f/completeFace.getMaterial().getTexture().getWidth(),completeFace.getVector3fs()[2].y*2048f/completeFace.getMaterial().getTexture().getWidth(),1);
                    completeFace.setTextureCoords(completeFace.getVector3fs()[2].x*2048f/completeFace.getMaterial().getTexture().getWidth(),completeFace.getVector3fs()[2].y*2048f/completeFace.getMaterial().getTexture().getWidth(), 2);
                    completeFace.setTextureCoords(completeFace.getVector3fs()[2].x*2048f/completeFace.getMaterial().getTexture().getWidth(),completeFace.getVector3fs()[0].y*2048f/completeFace.getMaterial().getTexture().getWidth(), 3);
                    break;
            }
        }
    }

    public void joinReversed(HashMap<Integer, Face> faces, int index, int side) {
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

    public int[] getIndexes(int verticeIndex) {
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