package com.ritualsoftheold.terra.mesher.resource;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Helps with building meshes for voxel data.
 *
 */
public class MeshContainer {

    private ArrayList<Vector3f> vector3fs;

    private ArrayList<Integer> indices;

    private ArrayList<Vector3f> texCoords;

    private ArrayList<ColorRGBA> colors;

    private ArrayList<Vector3f> normals;

    /**
     * Creates a new mesh container.
     */
    public MeshContainer() {
        vector3fs = new ArrayList<>();
        indices = new ArrayList<>();
        texCoords = new ArrayList<>();
        colors = new ArrayList<>();
        normals = new ArrayList<>();
    }
    public void color(ColorRGBA colorRGBA){
        colors.add(colorRGBA);
    }

    public void normals(Vector3f[] normal){normals.addAll(Arrays.asList(normal));}

    public void vertex(Vector3f[] vectors) {
        vector3fs.addAll(Arrays.asList(vectors));
    }

    public void vector(Vector3f vectors) {
        vector3fs.add(vectors);
    }

    public void triangle(int[] indexes) {
        for(int index : indexes){
            indices.add(index);
        }
    }

    public void texture(Vector3f[] vector2fs) {
        texCoords.addAll(Arrays.asList(vector2fs));
    }

    public ArrayList<Vector3f> getVertice() {
        return vector3fs;
    }

    public ArrayList<Integer> getIndices() {
        return indices;
    }

    public ArrayList<Vector3f> getTextureCoordinates() {
        return texCoords;
    }
    public ArrayList<Vector3f> getNormals(){return normals;}

    public ArrayList<ColorRGBA> getColors() {
        return colors;
    }

    public void clear(){
        texCoords.clear();
        indices.clear();
        vector3fs.clear();
        normals.clear();
        colors.clear();
    }
}