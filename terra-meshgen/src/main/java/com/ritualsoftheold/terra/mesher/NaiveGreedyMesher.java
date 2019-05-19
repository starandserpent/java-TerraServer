package com.ritualsoftheold.terra.mesher;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.jme3.math.Vector3f;
import com.ritualsoftheold.terra.core.buffer.BlockBuffer;
import com.ritualsoftheold.terra.core.material.TerraMaterial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Simple face-culling implementation. Some mesh generators might be able to
 * iterate only once, culling at same time; they'll need something custom.
 */

public class NaiveMesher {

    public HashMap<Integer, Multimap<TerraMaterial, Face>> cull(BlockBuffer buf) {

        HashMap<Integer, Multimap<TerraMaterial, Face>> sector = new HashMap<>();
        sector.put(0, ArrayListMultimap.create());
        sector.put(1, ArrayListMultimap.create());
        sector.put(2, ArrayListMultimap.create());
        sector.put(3, ArrayListMultimap.create());
        sector.put(4, ArrayListMultimap.create());
        sector.put(5, ArrayListMultimap.create());

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
                Multimap<TerraMaterial, Face> side = sector.get(0);
                face.setMaterial(material);
                face.setNormals(new Vector3f(-1, 0, 0), new Vector3f(-1, 0, 0), new Vector3f(-1, 0, 0), new Vector3f(-1, 0, 0));
                face.setVector3f(x, y, z + 1, 0);
                face.setVector3f(x, y + 1, z + 1, 1);
                face.setVector3f(x, y + 1, z, 2);
                face.setVector3f(x, y, z, 3);
                side.put(material, face);

                //Naive Meshing
                if(index > 64 && buf.get(index - 64).getTexture() != null) {
                    ArrayList<Face> faces = new ArrayList<>(side.get(material));
                    Collections.sort(faces);
                    if (faces.indexOf(face) > 1) {
                        int last = faces.indexOf(face) - 1;
                        Face previousFace = faces.get(last);
                        if (face.getVector3fs()[0].equals(previousFace.getVector3fs()[1]) &&
                                face.getVector3fs()[3].equals(previousFace.getVector3fs()[2])) {
                            previousFace.setVector3f(x, y + 1, z + 1, 1);
                            previousFace.setVector3f(x, y + 1, z, 2);
                            side.get(material).remove(face);
                        }
                    }
                }
            }

            // RIGHT
            if (x == 63 && buf.get(index + 64) != material || x < 63 && buf.get(index + 1).getTexture() == null) {
                Face face = new Face();
                face.setMaterial(material);
                face.setNormals(new Vector3f(1, 0, 0), new Vector3f(1, 0, 0), new Vector3f(1, 0, 0), new Vector3f(1, 0, 0));
                Multimap<TerraMaterial, Face> side = sector.get(1);
                face.setVector3f(x + 1, y, z, 0);
                face.setVector3f(x + 1, y + 1, z, 1);
                face.setVector3f(x + 1, y + 1, z + 1, 2);
                face.setVector3f(x + 1, y, z + 1, 3);
                side.put(material, face);

                if (index > 64 && buf.get(index - 64).getTexture() != null) {
                    ArrayList<Face> faces = new ArrayList<>(side.get(material));
                    Collections.sort(faces);
                    //Naive Meshing
                    if (faces.indexOf(face) > 1) {
                        int last = faces.indexOf(face) - 1;
                        Face previousFace = faces.get(last);
                        if (face.getVector3fs()[0].equals(previousFace.getVector3fs()[1]) &&
                                face.getVector3fs()[3].equals(previousFace.getVector3fs()[2])) {
                            previousFace.setVector3f(x + 1, y + 1, z, 1);
                            previousFace.setVector3f(x + 1, y + 1, z + 1, 2);
                            side.get(material).remove(face);
                        }
                    }
                }
            }

            // TOP
            if (y == 63 || buf.get(index + 64).getTexture() == null) {
                Face face = new Face();
                face.setMaterial(material);
                face.setNormals(new Vector3f(0, 1, 0), new Vector3f(0, 1, 0), new Vector3f(0, 1, 0), new Vector3f(0, 1, 0));
                Multimap<TerraMaterial, Face> side = sector.get(2);
                face.setVector3f(x, y + 1, z, 0);
                face.setVector3f(x, y + 1, z + 1, 1);
                face.setVector3f(x + 1, y + 1, z + 1, 2);
                face.setVector3f(x + 1, y + 1, z, 3);
                side.put(material, face);

                //Naive Meshing
                if (side.get(material).size() > 1 && index > 0 && buf.get(index - 1).getTexture() != null) {
                    ArrayList<Face> faces = new ArrayList<>(side.get(material));
                    int last = faces.size() - 2;
                    Face previousFace = faces.get(last);
                    if (face.getVector3fs()[0].equals(previousFace.getVector3fs()[3]) &&
                            face.getVector3fs()[1].equals(previousFace.getVector3fs()[2])) {
                        previousFace.setVector3f(x + 1, y + 1, z + 1, 2);
                        previousFace.setVector3f(x + 1, y + 1, z, 3);
                        side.get(material).remove(face);
                    }
                }
            }

            // BOTTOM
            if (y == 0 && buf.get(index + 64) != material || y > 0 && buf.get(index - 64).getTexture() == null) {
                Face face = new Face();
                face.setMaterial(material);
                face.setNormals(new Vector3f(0,-1,0),new Vector3f(0,-1,0),new Vector3f(0,-1,0),new Vector3f(0,-1,0));
                Multimap<TerraMaterial, Face> side = sector.get(3);
                face.setVector3f(x + 1, y, z, 0);
                face.setVector3f(x + 1, y, z + 1, 1);
                face.setVector3f(x, y, z + 1, 2);
                face.setVector3f(x, y, z, 3);
                side.put(material, face);

                //Naive Meshing
                if (side.get(material).size() > 1 && index > 0 && buf.get(index - 1).getTexture() != null) {
                    ArrayList<Face> faces = new ArrayList<>(side.get(material));
                    int last = faces.size() - 2;
                    Face previousFace = faces.get(last);
                    if (face.getVector3fs()[3].equals(previousFace.getVector3fs()[0]) &&
                            face.getVector3fs()[2].equals(previousFace.getVector3fs()[1])) {
                        previousFace.setVector3f(x + 1, y, z, 0);
                        previousFace.setVector3f(x + 1, y, z + 1, 1);
                        side.get(material).remove(face);
                    }
                }
            }

            // BACK
            if (z == 63 && buf.get(index + 64) != material || z < 63 && buf.get(index + 4096).getTexture() == null) {
                Face face = new Face();
                face.setMaterial(material);
                face.setNormals(new Vector3f(0,0,1),new Vector3f(0,0,1),new Vector3f(0,0,1),new Vector3f(0,0,1));
                Multimap<TerraMaterial, Face> side = sector.get(4);
                face.setVector3f(x + 1, y, z + 1, 0);
                face.setVector3f(x + 1, y + 1, z + 1, 1);
                face.setVector3f(x, y + 1, z + 1, 2);
                face.setVector3f(x, y, z + 1, 3);
                side.put(material, face);

                //Naive Meshing
                if (side.get(material).size() > 1 && index > 0 && buf.get(index - 1).getTexture() != null) {
                    ArrayList<Face> faces = new ArrayList<>(side.get(material));
                    int last = faces.size() - 2;
                    Face previousFace = faces.get(last);
                    if (face.getVector3fs()[3].equals(previousFace.getVector3fs()[0]) &&
                            face.getVector3fs()[2].equals(previousFace.getVector3fs()[1])) {
                        previousFace.setVector3f(x + 1, y, z + 1, 0);
                        previousFace.setVector3f(x + 1, y + 1, z + 1, 1);
                        side.get(material).remove(face);
                    }
                }
            }

            // FRONT
            if (z == 0 && buf.get(index + 64) != material || z > 0 && buf.get(index - 4096).getTexture() == null) {
                Face face = new Face();
                face.setMaterial(material);
                face.setNormals(new Vector3f(0,0,-1),new Vector3f(0,0,-1),new Vector3f(0,0,-1),new Vector3f(0,0,-1));
                Multimap<TerraMaterial, Face> side = sector.get(5);
                face.setVector3f(x, y, z, 0);
                face.setVector3f(x, y + 1, z, 1);
                face.setVector3f(x + 1, y + 1, z, 2);
                face.setVector3f(x + 1, y, z, 3);
                side.put(material, face);

                //Naive Meshing
                if (side.get(material).size() > 1 && index > 0 && buf.get(index - 1).getTexture() != null) {
                    ArrayList<Face> faces = new ArrayList<>(side.get(material));
                    int last = faces.size() - 2;
                    Face previousFace = faces.get(last);
                    if (face.getVector3fs()[0].equals(previousFace.getVector3fs()[3]) &&
                            face.getVector3fs()[1].equals(previousFace.getVector3fs()[2])) {
                        previousFace.setVector3f(x + 1, y + 1, z, 2);
                        previousFace.setVector3f(x + 1, y, z, 3);
                        side.get(material).remove(face);
                    }
                }
            }
            index++;
            buf.next();
        }
        return sector;
    }
}
