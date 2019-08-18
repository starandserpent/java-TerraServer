package com.ritualsoftheold.terra.mesher;

import com.jme3.math.Vector3f;
import com.ritualsoftheold.terra.offheap.chunk.ChunkLArray;

import java.util.*;

/**
 * Greedy mesher does culling and try to merge same blocks into bigger faces.
 */
public class GreedyMesher {
    private NaiveGreedyMesher culling;

    public GreedyMesher() {
        culling = new NaiveGreedyMesher();

    }

    public HashMap<Integer, HashMap<Integer, Face>> cull(ChunkLArray chunk) {
        assert chunk != null;

        if (chunk.isDifferent()) {
            // Generate mappings for culling
            return culling.cull(chunk);
        } else {
            HashMap<Integer, HashMap<Integer, Face>> cubeFaces = new HashMap<>();
            if (chunk.get(0).getTexture() != null) {

                // LEFT
                HashMap<Integer, Face> faces = new HashMap<>();
                Face face = new Face();

                face.setObject(chunk.get(0));
                face.setNormals(new Vector3f(-1, 0, 0), new Vector3f(-1, 0, 0), new Vector3f(-1, 0, 0), new Vector3f(-1, 0, 0));
                face.setVector3f(0, 0, 64, 0);
                face.setVector3f(0, 64, 64, 1);
                face.setVector3f(0, 64, 0, 2);
                face.setVector3f(0, 0, 0, 3);
                faces.put(0, face);

                cubeFaces.put(0, faces);

                // RIGHT
                faces = new HashMap<>();
                face = new Face();

                face.setObject(chunk.get(0));
                face.setNormals(new Vector3f(1, 0, 0), new Vector3f(1, 0, 0), new Vector3f(1, 0, 0), new Vector3f(1, 0, 0));
                face.setVector3f(64, 0, 0, 0);
                face.setVector3f(64, 64, 0, 1);
                face.setVector3f(64, 64, 64, 2);
                face.setVector3f(64, 0, 64, 3);
                faces.put(0, face);

                cubeFaces.put(1, faces);

                // TOP
                faces = new HashMap<>();
                face = new Face();

                face.setObject(chunk.get(0));
                face.setNormals(new Vector3f(0, 1, 0), new Vector3f(0, 1, 0), new Vector3f(0, 1, 0), new Vector3f(0, 1, 0));
                face.setVector3f(0, 64, 0, 0);
                face.setVector3f(0, 64, 64, 1);
                face.setVector3f(64, 64, 64, 2);
                face.setVector3f(64, 64, 0, 3);
                faces.put(0, face);

                cubeFaces.put(2, faces);

                // BOTTOM
                faces = new HashMap<>();
                face = new Face();

                face.setObject(chunk.get(0));
                face.setNormals(new Vector3f(0, -1, 0), new Vector3f(0, -1, 0), new Vector3f(0, -1, 0), new Vector3f(0, -1, 0));
                face.setVector3f(64, 0, 0, 0);
                face.setVector3f(64, 0, 64, 1);
                face.setVector3f(0, 0, 64, 2);
                face.setVector3f(0, 0, 0, 3);
                faces.put(0, face);

                cubeFaces.put(3, faces);

                // BACK
                faces = new HashMap<>();
                face = new Face();

                face.setObject(chunk.get(0));
                face.setNormals(new Vector3f(0, 0, 1), new Vector3f(0, 0, 1), new Vector3f(0, 0, 1), new Vector3f(0, 0, 1));
                face.setVector3f(64, 0, 64, 0);
                face.setVector3f(64, 64, 64, 1);
                face.setVector3f(0, 64, 64, 2);
                face.setVector3f(0, 0, 64, 3);
                faces.put(0, face);

                cubeFaces.put(4, faces);

                // FRONT
                faces = new HashMap<>();
                face = new Face();

                face.setObject(chunk.get(0));
                face.setNormals(new Vector3f(0, 0, -1), new Vector3f(0, 0, -1), new Vector3f(0, 0, -1), new Vector3f(0, 0, -1));
                face.setVector3f(0, 0, 0, 0);
                face.setVector3f(0, 64, 0, 1);
                face.setVector3f(64, 64, 0, 2);
                face.setVector3f(64, 0, 0, 3);
                faces.put(0, face);
                cubeFaces.put(5, faces);
            }
            
            chunk.free();
            cubeFaces.put(6, new HashMap<>());
            return cubeFaces;
        }
    }

    public void setTextureCoords(Collection<Face> faces, int side) {
        for (Face completeFace : faces) {
            switch (side) {
                case 0:
                case 1:
                    completeFace.setTextureCoords(completeFace.getVector3fs()[0].z * 2048f / completeFace.getObject().getTexture().getWidth(), completeFace.getVector3fs()[0].y * 2048f / completeFace.getObject().getTexture().getHeight(), 0);
                    completeFace.setTextureCoords(completeFace.getVector3fs()[0].z * 2048f / completeFace.getObject().getTexture().getWidth(), completeFace.getVector3fs()[2].y * 2048f / completeFace.getObject().getTexture().getHeight(), 1);
                    completeFace.setTextureCoords(completeFace.getVector3fs()[2].z * 2048f / completeFace.getObject().getTexture().getWidth(), completeFace.getVector3fs()[2].y * 2048f / completeFace.getObject().getTexture().getHeight(), 2);
                    completeFace.setTextureCoords(completeFace.getVector3fs()[2].z * 2048f / completeFace.getObject().getTexture().getWidth(), completeFace.getVector3fs()[0].y * 2048f / completeFace.getObject().getTexture().getHeight(), 3);
                    break;

                case 2:
                case 3:
                    completeFace.setTextureCoords(completeFace.getVector3fs()[0].x * 2048f / completeFace.getObject().getTexture().getWidth(), completeFace.getVector3fs()[0].z * 2048f / completeFace.getObject().getTexture().getHeight(), 0);
                    completeFace.setTextureCoords(completeFace.getVector3fs()[0].x * 2048f / completeFace.getObject().getTexture().getWidth(), completeFace.getVector3fs()[2].z * 2048f / completeFace.getObject().getTexture().getHeight(), 1);
                    completeFace.setTextureCoords(completeFace.getVector3fs()[2].x * 2048f / completeFace.getObject().getTexture().getWidth(), completeFace.getVector3fs()[2].z * 2048f / completeFace.getObject().getTexture().getHeight(), 2);
                    completeFace.setTextureCoords(completeFace.getVector3fs()[2].x * 2048f / completeFace.getObject().getTexture().getWidth(), completeFace.getVector3fs()[0].z * 2048f / completeFace.getObject().getTexture().getHeight(), 3);
                    break;

                case 4:
                case 5:
                    completeFace.setTextureCoords(completeFace.getVector3fs()[0].x * 2048f / completeFace.getObject().getTexture().getWidth(), completeFace.getVector3fs()[0].y * 2048f / completeFace.getObject().getTexture().getWidth(), 0);
                    completeFace.setTextureCoords(completeFace.getVector3fs()[0].x * 2048f / completeFace.getObject().getTexture().getWidth(), completeFace.getVector3fs()[2].y * 2048f / completeFace.getObject().getTexture().getWidth(), 1);
                    completeFace.setTextureCoords(completeFace.getVector3fs()[2].x * 2048f / completeFace.getObject().getTexture().getWidth(), completeFace.getVector3fs()[2].y * 2048f / completeFace.getObject().getTexture().getWidth(), 2);
                    completeFace.setTextureCoords(completeFace.getVector3fs()[2].x * 2048f / completeFace.getObject().getTexture().getWidth(), completeFace.getVector3fs()[0].y * 2048f / completeFace.getObject().getTexture().getWidth(), 3);
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
        if (face.getObject() == nextFace.getObject()) {
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
}