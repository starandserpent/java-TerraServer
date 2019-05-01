package com.ritualsoftheold.terra.mesher;

import com.ritualsoftheold.terra.core.material.TerraMaterial;
import com.ritualsoftheold.terra.core.buffer.BlockBuffer;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Simple face-culling implementation. Some mesh generators might be able to
 * iterate only once, culling at same time; they'll need something custom.
 */

public class CullingHelper {

    public HashMap<Integer, ArrayList<Face>> cull(BlockBuffer buf) {

        HashMap<Integer, ArrayList<Face>> sector = new HashMap<>();
        sector.put(0, new ArrayList<>());
        sector.put(1, new ArrayList<>());
        sector.put(2, new ArrayList<>());
        sector.put(3, new ArrayList<>());
        sector.put(4, new ArrayList<>());
        sector.put(5, new ArrayList<>());

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
            if (x == 0 || buf.get(index - 1).getTexture() == null) {
                Face face = new Face();
                ArrayList<Face> side =  sector.get(0);
                face.setMaterial(material);
                face.setVector3f(x, y, z + 1, 0);
                face.setVector3f(x, y + 1, z + 1, 1);
                face.setVector3f(x, y + 1, z, 2);
                face.setVector3f(x, y, z, 3);
                side.add(face);

                if (side.size() > 1 && buf.get(index - 1).getTexture() != null) {
                    int last = side.size() - 2;
                    Face previousFace = side.get(last);
                    if (face.getMaterial() == previousFace.getMaterial() &&
                            face.getVector3fs()[0].equals(previousFace.getVector3fs()[1]) &&
                            face.getVector3fs()[3].equals(previousFace.getVector3fs()[2])) {
                        previousFace.setVector3f(x, y + 1, z + 1, 1);
                        previousFace.setVector3f(x, y + 1, z, 2);
                        side.remove(side.size() - 1);
                    }
                }
            }

            // RIGHT
            if (x == 63 || buf.get(index + 1).getTexture() == null) {
                Face face = new Face();
                face.setMaterial(material);
                ArrayList<Face> side =  sector.get(1);
                face.setVector3f(x + 1, y, z, 0);
                face.setVector3f(x + 1, y + 1, z, 1);
                face.setVector3f(x + 1, y + 1, z + 1, 2);
                face.setVector3f(x + 1, y, z + 1, 3);
                side.add(face);

                //Greedy Meshing
                if (side.size() > 1 && buf.get(index - 1).getTexture() != null) {
                    int last = side.size() - 2;
                    Face previousFace = side.get(last);
                    if (face.getMaterial() == previousFace.getMaterial() &&
                            face.getVector3fs()[0].equals(previousFace.getVector3fs()[1]) &&
                            face.getVector3fs()[3].equals(previousFace.getVector3fs()[2])) {
                        previousFace.setVector3f(x + 1, y + 1, z, 1);
                        previousFace.setVector3f(x + 1, y + 1, z + 1, 2);
                        side.remove(side.size() - 1);
                    }
                }
            }

            // TOP
            if (y == 63 || buf.get(index + 64).getTexture() == null) {
                Face face = new Face();
                face.setMaterial(material);
                ArrayList<Face> side =  sector.get(2);
                face.setVector3f(x, y + 1, z, 0);
                face.setVector3f(x, y + 1, z + 1, 1);
                face.setVector3f(x + 1, y + 1, z + 1, 2);
                face.setVector3f(x + 1, y + 1, z, 3);
                side.add(face);

                //Greedy Meshing
                if (side.size() > 1 && buf.get(index - 4096).getTexture() != null) {
                    int last = side.size() - 2;
                    Face previousFace = side.get(last);
                    if (face.getMaterial() == previousFace.getMaterial() &&
                            face.getVector3fs()[0].equals(previousFace.getVector3fs()[3]) &&
                            face.getVector3fs()[1].equals(previousFace.getVector3fs()[2])) {
                        previousFace.setVector3f(x + 1, y + 1, z + 1, 2);
                        previousFace.setVector3f(x + 1, y + 1, z, 3);
                        side.remove(side.size() - 1);
                    }
                }
            }

            // BOTTOM
            if (y == 0 || buf.get(index - 64).getTexture() == null) {
                Face face = new Face();
                face.setMaterial(material);
                ArrayList<Face> side =  sector.get(3);
                face.setVector3f(x + 1, y, z, 0);
                face.setVector3f(x + 1, y, z + 1, 1);
                face.setVector3f(x, y, z + 1, 2);
                face.setVector3f(x, y, z, 3);
                side.add(face);

                //Greedy Meshing
                if (side.size() > 1 && buf.get(index - 1).getTexture() != null) {
                    int last = side.size() - 2;
                    Face previousFace = side.get(last);
                    if (face.getMaterial() == previousFace.getMaterial() &&
                            face.getVector3fs()[3].equals(previousFace.getVector3fs()[0]) &&
                            face.getVector3fs()[2].equals(previousFace.getVector3fs()[1])) {
                        previousFace.setVector3f(x + 1, y, z, 0);
                        previousFace.setVector3f(x + 1, y, z + 1, 1);
                        side.remove(side.size() - 1);
                    }
                }
            }

            // BACK
            if (z == 63 || buf.get(index + 4096).getTexture() == null) {
                Face face = new Face();
                face.setMaterial(material);
                ArrayList<Face> side =  sector.get(4);
                face.setVector3f(x + 1, y, z + 1, 0);
                face.setVector3f(x + 1, y + 1, z + 1, 1);
                face.setVector3f(x, y + 1, z + 1, 2);
                face.setVector3f(x, y, z + 1, 3);
                side.add(face);

                //Greedy Meshing
                if (side.size() > 1 && buf.get(index - 1).getTexture() != null) {
                    int last = side.size() - 2;
                    Face previousFace = side.get(last);
                    if (face.getMaterial() == previousFace.getMaterial() &&
                            face.getVector3fs()[3].equals(previousFace.getVector3fs()[0]) &&
                            face.getVector3fs()[2].equals(previousFace.getVector3fs()[1])) {
                        previousFace.setVector3f(x + 1, y, z + 1, 0);
                        previousFace.setVector3f(x + 1, y + 1, z + 1, 1);
                        side.remove(side.size() - 1);
                    }
                }
            }

            // FRONT
            if (z == 0 || buf.get(index - 4096).getTexture() == null) {
                Face face = new Face();
                face.setMaterial(material);
                ArrayList<Face> side =  sector.get(5);
                face.setVector3f(x, y, z, 0);
                face.setVector3f(x, y + 1, z, 1);
                face.setVector3f(x + 1, y + 1, z, 2);
                face.setVector3f(x + 1, y, z, 3);
                side.add(face);

                //Greedy Meshing
                if (side.size() > 1 && buf.get(index - 1).getTexture() != null) {
                    int last = side.size() - 2;
                    Face previousFace = side.get(last);
                    if (face.getMaterial() == previousFace.getMaterial() &&
                            face.getVector3fs()[0].equals(previousFace.getVector3fs()[3]) &&
                            face.getVector3fs()[1].equals(previousFace.getVector3fs()[2])) {
                        previousFace.setVector3f(x + 1, y + 1, z, 2);
                        previousFace.setVector3f(x + 1, y, z, 3);
                        side.remove(side.size() - 1);
                    }
                }
            }
            index++;
            buf.next();
        }
        return sector;
    }
}
