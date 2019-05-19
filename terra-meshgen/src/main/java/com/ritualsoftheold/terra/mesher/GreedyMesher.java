package com.ritualsoftheold.terra.mesher;

import com.google.common.collect.Multimap;
import com.ritualsoftheold.terra.core.buffer.BlockBuffer;
import com.ritualsoftheold.terra.core.material.TerraMaterial;
import com.ritualsoftheold.terra.mesher.resource.MeshContainer;
import com.ritualsoftheold.terra.mesher.resource.TextureManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Greedy mesher does culling and try to merge same blocks into bigger faces.
 */
public class GreedyMesher implements VoxelMesher {
    public GreedyMesher(){}

    @Override
    public void chunk(BlockBuffer buf, TextureManager textures, MeshContainer mesh) {
        System.out.println("Greedy meshing started.");
        long startTime = System.currentTimeMillis();
        assert buf != null;
        assert textures != null;
        assert mesh != null;

        NaiveGreedyMesher culling = new NaiveGreedyMesher();

        // Generate mappings for culling
        HashMap<Integer, Multimap<TerraMaterial, Face>> sector = culling.cull(buf);

        // Reset buffer to starting position
        buf.seek(0);
        int verticeIndex = 0;

        for(Integer key:sector.keySet()){
            TerraMaterial[] keys = new TerraMaterial[sector.get(key).keySet().toArray().length];
            sector.get(key).keySet().toArray(keys);
            for(TerraMaterial material:keys) {
                ArrayList<Face> rawFaces = new ArrayList<>(sector.get(key).removeAll(material));
                List<Face> faces = rawFaces.stream().distinct().collect(Collectors.toList());
                for(int i =0; i < faces.size(); i++) {
                    joinReversed(faces, i, key);
                }
                setTextureCoords(faces, key);
                verticeIndex = fillContainer(mesh, faces, verticeIndex);
            }
        }

        sector.clear();
        long finishTime = System.currentTimeMillis();
        long differenceTime = finishTime - startTime;
        System.out.println("Greedy meshing done: " + differenceTime + " milliseconds." );
        System.out.println("System time: " + System.currentTimeMillis() + " milliseconds." );

    }

    //Setting textures for mesh
    private static void setTextureCoords(List<Face> faces, int side) {
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
    private static int fillContainer(MeshContainer mesh, List<Face> faces, int verticeIndex) {
        for (Face completeFace : faces) {
            mesh.vertex(completeFace.getVector3fs());
            mesh.triangle(getIndexes(verticeIndex));
            mesh.texture(completeFace.getTextureCoords());
            mesh.normals(completeFace.getNormals());
            verticeIndex += 4;
        }

        return verticeIndex;
    }

    private static void joinReversed(List<Face> faces, int start, int side) {
        if (start + 1 < faces.size()) {
            Collections.sort(faces);
            Face face = faces.get(start);
            Face nextFace = faces.get(start + 1);
            switch (side) {
                case 0:
                    if (face.getVector3fs()[0].equals(nextFace.getVector3fs()[3]) && face.getVector3fs()[1].equals(nextFace.getVector3fs()[2])) {
                        face.setVector3f(nextFace.getVector3fs()[0], 0);
                        face.setVector3f(nextFace.getVector3fs()[1], 1);
                        faces.remove(nextFace);
                        joinReversed(faces, start, side);
                    }
                    break;

                case 1:
                    Collections.reverse(faces);
                    if (face.getVector3fs()[3].equals(nextFace.getVector3fs()[0]) && face.getVector3fs()[2].equals(nextFace.getVector3fs()[1])) {
                        face.setVector3f(nextFace.getVector3fs()[2], 2);
                        face.setVector3f(nextFace.getVector3fs()[3], 3);
                        faces.remove(nextFace);
                        joinReversed(faces, start, side);
                    }
                    break;

                case 2:
                case 3:
                case 4:
                case 5:
                    if (face.getVector3fs()[2].equals(nextFace.getVector3fs()[3]) && face.getVector3fs()[1].equals(nextFace.getVector3fs()[0])) {
                        face.setVector3f(nextFace.getVector3fs()[1], 1);
                        face.setVector3f(nextFace.getVector3fs()[2], 2);
                        faces.remove(nextFace);
                        joinReversed(faces, start, side);
                    }
                    break;
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