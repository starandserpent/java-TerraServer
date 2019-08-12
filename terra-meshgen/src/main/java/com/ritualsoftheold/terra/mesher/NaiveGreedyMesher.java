package com.ritualsoftheold.terra.mesher;

import com.jme3.math.Vector3f;
import com.ritualsoftheold.terra.core.material.TerraObject;
import com.ritualsoftheold.terra.offheap.chunk.ChunkLArray;

import java.util.HashMap;

/**
 * Simple face-culling implementation. Some mesh generators might be able to
 * iterate only once, culling at same time; they'll need something custom.
 */

class NaiveGreedyMesher {
    private HashMap<Integer, HashMap<Integer, Face>> sector;

    NaiveGreedyMesher() {
        sector = new HashMap<>();
        for (int i = 0; i < 7; i++) {
            sector.put(i, new HashMap<>());
        }
    }

    HashMap<Integer, HashMap<Integer, Face>> cull(ChunkLArray lArray) {
        //Creates voxels from BlockBuffer and set its object
        for (int i = 0; i < ChunkLArray.CHUNK_SIZE; i++) {
            TerraObject object = lArray.get(i);
            if (object.getTexture() == null) {
                continue;
            }

            if (object.hasMesh()) {
                HashMap<Integer, Face> resources = sector.get(6);
                Face face = new Face();
                face.setObject(object);
                resources.put(i, face);
                continue;
            }

            //Position of current voxel
            int z = i / 4096;
            int y = (i - 4096 * z) / 64;
            int x = i % 64;

            //Culls face if there is in Left, Right, Top, Bottom, Back, Front exiting face
            //Left, Bottom, Back faces are reversed

            // LEFT
            /*&& buf.get(i + 64) != object*/
            if (x == 0 || lArray.get(i - 1).getTexture() == null || lArray.get(i - 1).hasMesh()) {
                Face face = new Face();
                HashMap<Integer, Face> side = sector.get(0);
                face.setObject(object);
                face.setNormals(new Vector3f(-1, 0, 0), new Vector3f(-1, 0, 0), new Vector3f(-1, 0, 0), new Vector3f(-1, 0, 0));
                face.setVector3f(x, y, z + 1, 0);
                face.setVector3f(x, y + 1, z + 1, 1);
                face.setVector3f(x, y + 1, z, 2);
                face.setVector3f(x, y, z, 3);
                side.put(i, face);

                //Naive Greedy Meshing
                if (i > 4096) {
                    Face previousFace = side.get(i - 4096);
                    if (previousFace != null && previousFace.getObject() == face.getObject()) {
                        if (face.getVector3fs()[3].equals(previousFace.getVector3fs()[0]) &&
                                face.getVector3fs()[2].equals(previousFace.getVector3fs()[1])) {
                            face.setVector3f(previousFace.getVector3fs()[2], 2);
                            face.setVector3f(previousFace.getVector3fs()[3], 3);
                            side.remove(i - 4096);
                        }
                    }
                }
            }

            // RIGHT
            /*&& buf.get(i + 64) != object*/
            if (x == 63 || lArray.get(i + 1).getTexture() == null || lArray.get(i + 1).hasMesh()) {
                Face face = new Face();
                face.setObject(object);
                face.setNormals(new Vector3f(1, 0, 0), new Vector3f(1, 0, 0), new Vector3f(1, 0, 0), new Vector3f(1, 0, 0));
                HashMap<Integer, Face> side = sector.get(1);
                face.setVector3f(x + 1, y, z, 0);
                face.setVector3f(x + 1, y + 1, z, 1);
                face.setVector3f(x + 1, y + 1, z + 1, 2);
                face.setVector3f(x + 1, y, z + 1, 3);
                side.put(i, face);

                //Naive Greedy Meshing
                if (i > 4096) {
                    Face previousFace = side.get(i - 4096);
                    if (previousFace != null && previousFace.getObject() == face.getObject()) {
                        if (face.getVector3fs()[0].equals(previousFace.getVector3fs()[3]) &&
                                face.getVector3fs()[1].equals(previousFace.getVector3fs()[2])) {
                            face.setVector3f(previousFace.getVector3fs()[0], 0);
                            face.setVector3f(previousFace.getVector3fs()[1], 1);
                            side.remove(i - 4096);
                        }
                    }
                }
            }

            // TOP
            if (y == 63 || lArray.get(i + 64).getTexture() == null || lArray.get(i + 64).hasMesh()) {
                Face face = new Face();
                face.setObject(object);
                face.setNormals(new Vector3f(0, 1, 0), new Vector3f(0, 1, 0), new Vector3f(0, 1, 0), new Vector3f(0, 1, 0));
                HashMap<Integer, Face> side = sector.get(2);
                face.setVector3f(x, y + 1, z, 0);
                face.setVector3f(x, y + 1, z + 1, 1);
                face.setVector3f(x + 1, y + 1, z + 1, 2);
                face.setVector3f(x + 1, y + 1, z, 3);
                side.put(i, face);

                //Naive Greedy Meshing
                if (i > 1) {
                    Face previousFace = side.get(i - 1);
                    if (previousFace != null && previousFace.getObject() == face.getObject()) {
                        if (face.getVector3fs()[0].equals(previousFace.getVector3fs()[3]) &&
                                face.getVector3fs()[1].equals(previousFace.getVector3fs()[2])) {
                            face.setVector3f(previousFace.getVector3fs()[0], 0);
                            face.setVector3f(previousFace.getVector3fs()[1], 1);
                            side.remove(i - 1);
                        }
                    }
                }
            }

            // BOTTOM
            if (y == 0 || y > 0 && lArray.get(i - 64).getTexture() == null || lArray.get(i - 64).hasMesh()) {
                Face face = new Face();
                face.setObject(object);
                face.setNormals(new Vector3f(0, -1, 0), new Vector3f(0, -1, 0), new Vector3f(0, -1, 0), new Vector3f(0, -1, 0));
                HashMap<Integer, Face> side = sector.get(3);
                face.setVector3f(x + 1, y, z, 0);
                face.setVector3f(x + 1, y, z + 1, 1);
                face.setVector3f(x, y, z + 1, 2);
                face.setVector3f(x, y, z, 3);
                side.put(i, face);

                //Naive Greedy Meshing
                if (i > 1) {
                    Face previousFace = side.get(i - 1);
                    if (previousFace != null && previousFace.getObject() == face.getObject()) {
                        if (face.getVector3fs()[3].equals(previousFace.getVector3fs()[0]) &&
                                face.getVector3fs()[2].equals(previousFace.getVector3fs()[1])) {
                            face.setVector3f(previousFace.getVector3fs()[3], 3);
                            face.setVector3f(previousFace.getVector3fs()[2], 2);
                            side.remove(i - 1);
                        }
                    }
                }
            }
            //&& buf.get(i + 64) != object*/ || z < 63
            // BACK
            if (z == 63 || lArray.get(i + 4096).getTexture() == null || lArray.get(i + 4096).hasMesh()) {
                Face face = new Face();
                face.setObject(object);
                face.setNormals(new Vector3f(0, 0, 1), new Vector3f(0, 0, 1), new Vector3f(0, 0, 1), new Vector3f(0, 0, 1));
                HashMap<Integer, Face> side = sector.get(4);
                face.setVector3f(x + 1, y, z + 1, 0);
                face.setVector3f(x + 1, y + 1, z + 1, 1);
                face.setVector3f(x, y + 1, z + 1, 2);
                face.setVector3f(x, y, z + 1, 3);
                side.put(i, face);

                //Naive Greedy Meshing
                if (i > 1) {
                    Face previousFace = side.get(i - 1);
                    if (previousFace != null && previousFace.getObject() == face.getObject()) {
                        if (face.getVector3fs()[3].equals(previousFace.getVector3fs()[0]) &&
                                face.getVector3fs()[2].equals(previousFace.getVector3fs()[1])) {
                            face.setVector3f(previousFace.getVector3fs()[3], 3);
                            face.setVector3f(previousFace.getVector3fs()[2], 2);
                            side.remove(i - 1);
                        }
                    }
                }
            }

            // FRONT
            if (z == 0 || lArray.get(i - 4096).getTexture() == null || lArray.get(i - 4096).hasMesh()) {
                Face face = new Face();
                face.setObject(object);
                face.setNormals(new Vector3f(0, 0, -1), new Vector3f(0, 0, -1), new Vector3f(0, 0, -1), new Vector3f(0, 0, -1));
                HashMap<Integer, Face> side = sector.get(5);
                face.setVector3f(x, y, z, 0);
                face.setVector3f(x, y + 1, z, 1);
                face.setVector3f(x + 1, y + 1, z, 2);
                face.setVector3f(x + 1, y, z, 3);
                side.put(i, face);

                //Naive Greedy Meshing
                if (i > 1) {
                    Face previousFace = side.get(i - 1);
                    if (previousFace != null && previousFace.getObject() == face.getObject()) {
                        if (face.getVector3fs()[0].equals(previousFace.getVector3fs()[3]) &&
                                face.getVector3fs()[1].equals(previousFace.getVector3fs()[2])) {
                            face.setVector3f(previousFace.getVector3fs()[0], 0);
                            face.setVector3f(previousFace.getVector3fs()[1], 1);
                            side.remove(i - 1);
                        }
                    }
                }
            }
        }
        return sector;
    }
}