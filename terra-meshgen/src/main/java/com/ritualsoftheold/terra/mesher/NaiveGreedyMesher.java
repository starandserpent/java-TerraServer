package com.ritualsoftheold.terra.mesher;

import com.jme3.math.Vector3f;
import com.ritualsoftheold.terra.core.buffer.BlockBuffer;
import com.ritualsoftheold.terra.core.material.TerraMaterial;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Simple face-culling implementation. Some mesh generators might be able to
 * iterate only once, culling at same time; they'll need something custom.
 */

public class NaiveGreedyMesher {

    public HashMap<Integer, HashMap<Integer, Face>> cull(BlockBuffer buf) {
        long startTime = System.currentTimeMillis();
        HashMap<Integer, HashMap<Integer, Face>> sector = new HashMap<>();

        for (int i = 0; i < 6; i++){
            sector.put(i, new HashMap<>());
        }

        int index = 0;
        buf.seek(0);
        //Creates voxels from BlockBuffer and set its material
        while (buf.hasNext()) {
            TerraMaterial material = buf.read();
            if (material.getTexture() == null) { // TODO better AIR check
                buf.next();
                index++;
                continue;
            }

            //Position of current voxel
            int z = index / 4096;
            int y = (index - 4096 * z) / 64;
            int x = index % 64;

            //Culls face if there is in Left, Right, Top, Bottom, Back, Front exiting face
            //Left, Bottom, Back faces are reversed

            // LEFT
            if (x == 0 && buf.get(index + 64) != material || x > 0 && buf.get(index - 1).getTexture() == null) {
                Face face = new Face();
                HashMap<Integer, Face> side = sector.get(0);
                face.setMaterial(material);
                face.setNormals(new Vector3f(-1, 0, 0), new Vector3f(-1, 0, 0), new Vector3f(-1, 0, 0), new Vector3f(-1, 0, 0));
                face.setVector3f(x, y, z + 1, 0);
                face.setVector3f(x, y + 1, z + 1, 1);
                face.setVector3f(x, y + 1, z, 2);
                face.setVector3f(x, y, z, 3);
                side.put(index, face);

                //Naive Greedy Meshing
                if (index > 4096) {
                    Face previousFace = side.get(index - 4096);
                    if (previousFace != null && previousFace.getMaterial() == face.getMaterial()) {
                        if (face.getVector3fs()[3].equals(previousFace.getVector3fs()[0]) &&
                                face.getVector3fs()[2].equals(previousFace.getVector3fs()[1])) {
                            face.setVector3f(previousFace.getVector3fs()[2], 2);
                            face.setVector3f(previousFace.getVector3fs()[3], 3);
                            side.remove(index - 4096);
                        }
                    }
                }
            }

            // RIGHT
            if (x == 63 && buf.get(index + 64) != material || x < 63 && buf.get(index + 1).getTexture() == null) {
                Face face = new Face();
                face.setMaterial(material);
                face.setNormals(new Vector3f(1, 0, 0), new Vector3f(1, 0, 0), new Vector3f(1, 0, 0), new Vector3f(1, 0, 0));
                HashMap<Integer, Face> side = sector.get(1);
                face.setVector3f(x + 1, y, z, 0);
                face.setVector3f(x + 1, y + 1, z, 1);
                face.setVector3f(x + 1, y + 1, z + 1, 2);
                face.setVector3f(x + 1, y, z + 1, 3);
                side.put(index, face);

                //Naive Greedy Meshing
                if (index > 4096) {
                    Face previousFace = side.get(index - 4096);
                    if (previousFace != null && previousFace.getMaterial() == face.getMaterial()) {
                        if(face.getVector3fs()[0].equals(previousFace.getVector3fs()[3]) &&
                                face.getVector3fs()[1].equals(previousFace.getVector3fs()[2])) {
                            face.setVector3f(previousFace.getVector3fs()[0], 0);
                            face.setVector3f(previousFace.getVector3fs()[1], 1);
                            side.remove(index - 4096);
                        }
                    }
                }
            }

            // TOP
            if (y == 63 || buf.get(index + 64).getTexture() == null) {
                Face face = new Face();
                face.setMaterial(material);
                face.setNormals(new Vector3f(0, 1, 0), new Vector3f(0, 1, 0), new Vector3f(0, 1, 0), new Vector3f(0, 1, 0));
                HashMap<Integer, Face> side = sector.get(2);
                face.setVector3f(x, y + 1, z, 0);
                face.setVector3f(x, y + 1, z + 1, 1);
                face.setVector3f(x + 1, y + 1, z + 1, 2);
                face.setVector3f(x + 1, y + 1, z, 3);
                side.put(index, face);

                //Naive Greedy Meshing
                if (index > 1) {
                    Face previousFace = side.get(index - 1);
                    if (previousFace != null && previousFace.getMaterial() == face.getMaterial()) {
                        if(face.getVector3fs()[0].equals(previousFace.getVector3fs()[3]) &&
                                face.getVector3fs()[1].equals(previousFace.getVector3fs()[2])) {
                            face.setVector3f(previousFace.getVector3fs()[0], 0);
                            face.setVector3f(previousFace.getVector3fs()[1], 1);
                            side.remove(index - 1);
                        }
                    }
                }
            }

            // BOTTOM
            if (y == 0 && buf.get(index + 64) != material || y > 0 && buf.get(index - 64).getTexture() == null) {
                Face face = new Face();
                face.setMaterial(material);
                face.setNormals(new Vector3f(0,-1,0),new Vector3f(0,-1,0),new Vector3f(0,-1,0),new Vector3f(0,-1,0));
                HashMap<Integer, Face> side = sector.get(3);
                face.setVector3f(x + 1, y, z, 0);
                face.setVector3f(x + 1, y, z + 1, 1);
                face.setVector3f(x, y, z + 1, 2);
                face.setVector3f(x, y, z, 3);
                side.put(index, face);

                //Naive Greedy Meshing
                if (index > 1) {
                    Face previousFace = side.get(index - 1);
                    if (previousFace != null && previousFace.getMaterial() == face.getMaterial()) {
                        if(face.getVector3fs()[3].equals(previousFace.getVector3fs()[0]) &&
                                face.getVector3fs()[2].equals(previousFace.getVector3fs()[1])) {
                            face.setVector3f(previousFace.getVector3fs()[3], 3);
                            face.setVector3f(previousFace.getVector3fs()[2], 2);
                            side.remove(index - 1);
                        }
                    }
                }
            }

            // BACK
            if (z == 63 && buf.get(index + 64) != material || z < 63 && buf.get(index + 4096).getTexture() == null) {
                Face face = new Face();
                face.setMaterial(material);
                face.setNormals(new Vector3f(0,0,1),new Vector3f(0,0,1),new Vector3f(0,0,1),new Vector3f(0,0,1));
                HashMap<Integer, Face> side = sector.get(4);
                face.setVector3f(x + 1, y, z + 1, 0);
                face.setVector3f(x + 1, y + 1, z + 1, 1);
                face.setVector3f(x, y + 1, z + 1, 2);
                face.setVector3f(x, y, z + 1, 3);
                side.put(index, face);

                //Naive Greedy Meshing
                if (index > 1) {
                    Face previousFace = side.get(index - 1);
                    if (previousFace != null && previousFace.getMaterial() == face.getMaterial()) {
                        if(face.getVector3fs()[3].equals(previousFace.getVector3fs()[0]) &&
                                face.getVector3fs()[2].equals(previousFace.getVector3fs()[1])) {
                            face.setVector3f(previousFace.getVector3fs()[3], 3);
                            face.setVector3f(previousFace.getVector3fs()[2], 2);
                            side.remove(index - 1);
                        }
                    }
                }
            }

            // FRONT
            if (z == 0 && buf.get(index + 64) != material || z > 0 && buf.get(index - 4096).getTexture() == null) {
                Face face = new Face();
                face.setMaterial(material);
                face.setNormals(new Vector3f(0,0,-1),new Vector3f(0,0,-1),new Vector3f(0,0,-1),new Vector3f(0,0,-1));
                HashMap<Integer, Face> side = sector.get(5);
                face.setVector3f(x, y, z, 0);
                face.setVector3f(x, y + 1, z, 1);
                face.setVector3f(x + 1, y + 1, z, 2);
                face.setVector3f(x + 1, y, z, 3);
                side.put(index, face);

                //Naive Greedy Meshing
                if (index > 1) {
                    Face previousFace = side.get(index - 1);
                    if (previousFace != null && previousFace.getMaterial() == face.getMaterial()) {
                        if(face.getVector3fs()[0].equals(previousFace.getVector3fs()[3]) &&
                                face.getVector3fs()[1].equals(previousFace.getVector3fs()[2])) {
                            face.setVector3f(previousFace.getVector3fs()[0], 0);
                            face.setVector3f(previousFace.getVector3fs()[1], 1);
                            side.remove(index - 1);
                        }
                    }
                }
            }
            index++;
            buf.next();
        }
        long stopTime = System.currentTimeMillis();
        System.out.println("Naive greedy meshing done: " + (stopTime - startTime) + " milliseconds.");
        return sector;
    }
}
