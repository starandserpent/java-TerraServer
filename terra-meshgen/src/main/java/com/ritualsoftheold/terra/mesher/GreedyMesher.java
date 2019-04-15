package com.ritualsoftheold.terra.mesher;

import com.ritualsoftheold.terra.core.buffer.BlockBuffer;
import com.ritualsoftheold.terra.core.material.TerraMaterial;
import com.ritualsoftheold.terra.core.material.TerraTexture;
import com.ritualsoftheold.terra.mesher.resource.TextureManager;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.data.BufferWithFormat;

import java.util.ArrayList;

/**
 * Greedy mesher does culling and try to merge same blocks into bigger faces.
 *
 */
public class GreedyMesher implements VoxelMesher {
    private CullingHelper culling;
    private int verticeIndex;

    public GreedyMesher(CullingHelper culling) {
        this.culling = culling;
    }

    public GreedyMesher() {
        this(new CullingHelper());
    }

    @Override
    public void chunk(BlockBuffer buf, TextureManager textures, MeshContainer mesh) {
        assert buf != null;
        assert textures != null;
        assert mesh != null;

        // Generate mappings for culling
        Face[][] faces = culling.cull(buf);

        // Reset buffer to starting position
        buf.seek(0);

        ArrayList<Face> front = new ArrayList<>();
        ArrayList<Face> back = new ArrayList<>();
        ArrayList<Face> right = new ArrayList<>();
        ArrayList<Face> left = new ArrayList<>();
        ArrayList<Face> top = new ArrayList<>();
        ArrayList<Face> bottom = new ArrayList<>();

        verticeIndex = 0;

        for(Face[] voxel:faces){
            if(voxel != null) {
                for(int i = 0; i < voxel.length; i++){
                    if(voxel[i] != null) {
                        Face face = voxel[i];
                        switch (i) {
                            case 0:
                                left.add(voxel[i]);
                                break;
                            case 1:
                                right.add(voxel[i]);
                                break;
                            case 2:
                                top.add(voxel[i]);
                                break;
                            case 3:
                                bottom.add(voxel[i]);
                                break;
                            case 4:
                                back.add(voxel[i]);
                                break;
                            case 5:
                                front.add(face);
                                break;
                        }
                    }
                }
            }
        }

        fillContainer(mesh, back);
        fillContainer(mesh, right);
        fillContainer(mesh, left);
        fillContainer(mesh, top);
        fillContainer(mesh, bottom);
        fillContainer(mesh, front);
    }

    private void fillContainer(MeshContainer mesh, ArrayList<Face> faces) {
        for(int i = 0 ; i < faces.size() - 1; i++) {
            joinFacesHorizonaly(faces, i);
        }

        for(int i = 0 ; i < faces.size() - 1; i++) {
            joinFacesVerticaly(faces, i);
        }

        for (Face completeFace : faces) {
            setIndexes(completeFace, verticeIndex);
            verticeIndex += 4;

            mesh.vector(completeFace.getVector3f());
            mesh.triangle(completeFace.getIndexes());
            mesh.texture(completeFace.getTextureCoords());
        }
    }

    private void joinFacesHorizonaly(ArrayList<Face> faces, int start) {
        if(start + 1< faces.size()) {
            Face face = faces.get(start);
            Face nextFace = faces.get(start + 1);
            if (face.getVector3f()[3].equals(nextFace.getVector3f()[0]) && face.getVector3f()[2].equals(nextFace.getVector3f()[1])) {
                face.setVector3f(nextFace.getVector3f()[3], 3);
                face.setVector3f(nextFace.getVector3f()[2], 2);
                faces.remove(nextFace);
                joinFacesHorizonaly(faces, start);
            }else if (face.getVector3f()[0].equals(nextFace.getVector3f()[3]) && face.getVector3f()[1].equals(nextFace.getVector3f()[2])){
                face.setVector3f(nextFace.getVector3f()[0], 0);
                face.setVector3f(nextFace.getVector3f()[1], 1);
                faces.remove(nextFace);
                joinFacesHorizonaly(faces, start);
            }else if (face.getVector3f()[1].equals(nextFace.getVector3f()[0]) && face.getVector3f()[2].equals(nextFace.getVector3f()[3])){
                face.setVector3f(nextFace.getVector3f()[1], 1);
                face.setVector3f(nextFace.getVector3f()[2], 2);
                faces.remove(nextFace);
                joinFacesHorizonaly(faces, start);
            }
        }
    }

    private void joinFacesVerticaly(ArrayList<Face> faces, int start) {
        if(start + 1< faces.size()) {
            Face face = faces.get(start);
            Face nextFace = faces.get(start + 1);
            if (face.getVector3f()[2].equals(nextFace.getVector3f()[1]) && face.getVector3f()[3].equals(nextFace.getVector3f()[0])){
                face.setVector3f(nextFace.getVector3f()[2], 2);
                face.setVector3f(nextFace.getVector3f()[3], 3);
                faces.remove(nextFace);
                joinFacesHorizonaly(faces, start);
            }else if (face.getVector3f()[1].equals(nextFace.getVector3f()[2]) && face.getVector3f()[0].equals(nextFace.getVector3f()[3])){
                face.setVector3f(nextFace.getVector3f()[1], 1);
                face.setVector3f(nextFace.getVector3f()[0], 0);
                faces.remove(nextFace);
                joinFacesHorizonaly(faces, start);
            } else if (face.getVector3f()[2].equals(nextFace.getVector3f()[3]) && face.getVector3f()[1].equals(nextFace.getVector3f()[0])) {
                face.setVector3f(nextFace.getVector3f()[2], 2);
                face.setVector3f(nextFace.getVector3f()[1], 1);
                faces.remove(nextFace);
                joinFacesVerticaly(faces, start);
            }
        }
    }

    private void setIndexes(Face face, int verticeIndex) {
        face.setIndexes(verticeIndex, 0);
        face.setIndexes(verticeIndex + 2, 1);
        face.setIndexes(verticeIndex + 3, 2);
        face.setIndexes(verticeIndex + 2, 3);
        face.setIndexes(verticeIndex, 4);
        face.setIndexes(verticeIndex + 1, 5);
    }

    @Override
    public void cube(int id, float scale, TextureManager textures, MeshContainer mesh) {

    }
}
