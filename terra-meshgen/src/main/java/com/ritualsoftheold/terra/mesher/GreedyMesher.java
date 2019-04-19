package com.ritualsoftheold.terra.mesher;

import com.jme3.math.Vector2f;
import com.ritualsoftheold.terra.core.buffer.BlockBuffer;
import com.ritualsoftheold.terra.core.material.TerraMaterial;
import com.ritualsoftheold.terra.core.material.TerraTexture;
import com.ritualsoftheold.terra.mesher.resource.TextureManager;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.data.BufferWithFormat;

import java.util.ArrayList;
import java.util.HashMap;

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
        Voxel[] voxels = culling.cull(buf, mesh);

        // Reset buffer to starting position
        buf.seek(0);

        HashMap<Integer, ArrayList<Face>> entity = new HashMap<>();
        entity.put(0, new ArrayList<>());
        entity.put(1, new ArrayList<>());
        entity.put(2, new ArrayList<>());
        entity.put(3, new ArrayList<>());
        entity.put(4, new ArrayList<>());
        entity.put(5, new ArrayList<>());
        verticeIndex = 0;

        for(Voxel voxel:voxels) {
            if (voxel != null) {
                for (int i = 0; i < 6; i++) {
                    if (voxel.getFace(i) != null) {
                        Face face = voxel.getFace(i);
                        entity.get(i).add(face);
                    }
                }
            }
        }

        for(Integer key:entity.keySet()){
            joinFaces(entity.get(key));
            setTextureCoords(entity.get(key), key);
            fillContainer(mesh, entity.get(key));
        }
    }

    private void joinFaces(ArrayList<Face> faces) {
        for (int i = 0; i < faces.size(); i++) {
            joinFacesHorizonaly(faces, i);
        }

        for (int i = 0; i < faces.size(); i++) {
            joinFacesVerticaly(faces, i);
        }
    }

    private void setTextureCoords(ArrayList<Face> faces, int side){
        for (Face completeFace : faces) {
            switch (side) {
            /*    case 0:
                    completeFace.setTextureCoords(0.25f, 0);
                    completeFace.setTextureCoords(0, 0);
                    completeFace.setTextureCoords(0, 0.25f);
                    completeFace.setTextureCoords(0.25f, 0.25f);
                    break;
                case 1:
                    completeFace.setTextureCoords(0.5f, 0.25f);
                    completeFace.setTextureCoords(0.25f, 0.25f);
                    completeFace.setTextureCoords(0.25f, 0.5f);
                    completeFace.setTextureCoords(0.5f, 0.5f);
                    break;
                case 2:
                    completeFace.setTextureCoords(0.25f, 0);
                    completeFace.setTextureCoords(0.25f, 0.25f);
                    completeFace.setTextureCoords(0.5f, 0.25f);
                    completeFace.setTextureCoords(2, 2);
                    break;
                case 3:
                    completeFace.setTextureCoords(2, 0);
                    completeFace.setTextureCoords(0, 0);
                    completeFace.setTextureCoords(0, 2);
                    completeFace.setTextureCoords(2, 2);
                    break;
                case 4:
                    completeFace.setTextureCoords(2, 0);
                    completeFace.setTextureCoords(0, 0);
                    completeFace.setTextureCoords(0, 2);
                    completeFace.setTextureCoords(2, 2);
                    break;

                case 5:
                    completeFace.setTextureCoords(2, 0);
                    completeFace.setTextureCoords(0, 0);
                    completeFace.setTextureCoords(0, 2);
                    completeFace.setTextureCoords(2, 2);
                    break;
*/

               default:
                completeFace.setTextureCoords(1, 0);

                completeFace.setTextureCoords(0, 0);

                completeFace.setTextureCoords(0, 1);

                completeFace.setTextureCoords(1, 1);

                break;
            }
        }

    }

    private void fillContainer(MeshContainer mesh, ArrayList<Face> faces) {
        for (Face completeFace : faces) {
            mesh.vector(completeFace.getVector3f());
            mesh.triangle(getIndexes(verticeIndex));
            mesh.texture(completeFace.getTextureCoords());
            verticeIndex += 4;
        }
    }

    private void joinFacesHorizonaly(ArrayList<Face> faces, int start) {
        if(start + 1< faces.size()) {
            Face face = faces.get(start);
            Face nextFace = faces.get(start + 1);
            if(face.getTexture() == nextFace.getTexture()) {
                if (face.getVector3f()[3].equals(nextFace.getVector3f()[0]) && face.getVector3f()[2].equals(nextFace.getVector3f()[1])) {
                    face.setVector3f(nextFace.getVector3f()[3], 3);
                    face.setVector3f(nextFace.getVector3f()[2], 2);
                    faces.remove(nextFace);
                    joinFacesHorizonaly(faces, start);
                } else if (face.getVector3f()[0].equals(nextFace.getVector3f()[3]) && face.getVector3f()[1].equals(nextFace.getVector3f()[2])) {
                    face.setVector3f(nextFace.getVector3f()[0], 0);
                    face.setVector3f(nextFace.getVector3f()[1], 1);
                    faces.remove(nextFace);
                    joinFacesHorizonaly(faces, start);
                } else if (face.getVector3f()[1].equals(nextFace.getVector3f()[0]) && face.getVector3f()[2].equals(nextFace.getVector3f()[3])) {
                    face.setVector3f(nextFace.getVector3f()[1], 1);
                    face.setVector3f(nextFace.getVector3f()[2], 2);
                    faces.remove(nextFace);
                    joinFacesHorizonaly(faces, start);
                }
            }
        }
    }

    private void joinFacesVerticaly(ArrayList<Face> faces, int start) {
        if(start + 1< faces.size()) {
            Face face = faces.get(start);
            Face nextFace = faces.get(start + 1);
            if(face.getTexture() == nextFace.getTexture()) {
                if (face.getVector3f()[2].equals(nextFace.getVector3f()[1]) && face.getVector3f()[3].equals(nextFace.getVector3f()[0])) {
                    face.setVector3f(nextFace.getVector3f()[2], 2);
                    face.setVector3f(nextFace.getVector3f()[3], 3);
                    faces.remove(nextFace);
                    joinFacesHorizonaly(faces, start);
                } else if (face.getVector3f()[1].equals(nextFace.getVector3f()[2]) && face.getVector3f()[0].equals(nextFace.getVector3f()[3])) {
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
    }

    private int[] getIndexes(int verticeIndex) {
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
