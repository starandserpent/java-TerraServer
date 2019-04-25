package com.ritualsoftheold.terra.mesher;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.ritualsoftheold.terra.core.material.TerraTexture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Helps with building meshes for voxel data.
 *
 */
public class MeshContainer {
    private TerraTexture[][][] textures;

    private HashMap<TerraTexture, Integer> textureTypes;

    private ArrayList<Vector3f> vector3fs;

    private ArrayList<Integer> indices;

    private ArrayList<Vector3f> texCoords;

    private ArrayList<ColorRGBA> colors;

    /**
     * Creates a new mesh container.
     */
    public MeshContainer() {
        vector3fs = new ArrayList<>();
        indices = new ArrayList<>();
        texCoords = new ArrayList<>();
        textures = new TerraTexture[64][64][64];
        textureTypes = new HashMap<>();
        colors = new ArrayList<>();
    }

    public void color(ColorRGBA color){
        colors.add(color);
    }

    public void vector(Vector3f vector){
        vector3fs.add(vector);
    }

    public void vector(Vector3f[] vectors) {
        vector3fs.addAll(Arrays.asList(vectors));
    }

    public void triangle(int[] indexes) {
        for(int index : indexes){
            indices.add(index);
        }
    }

    public void texture(Vector3f[] vector3fs) {
        texCoords.addAll(Arrays.asList(vector3fs));
    }

    public void setTextures(int nX, int nY, int nZ, TerraTexture texture) {
        textures[nZ][nY][nX] = texture;

        int i = 1;
        if(textureTypes.get(texture) != null) {
            i = textureTypes.get(texture);
            i++;
        }
        textureTypes.put(texture, i);
    }

    public TerraTexture[][][] getTextures(){
        return textures;
    }

    public TerraTexture getMainTexture() {
        int max = 0;
        TerraTexture texture = null;
        for (TerraTexture key : textureTypes.keySet()) {
            if(textureTypes.get(key) == 0) {
                textureTypes.remove(texture);
            } else if (textureTypes.get(key) > max) {
                max = textureTypes.get(key);
                texture = key;
            }
        }

        return texture;
    }

    public int getTextureTypes() {
        return textureTypes.size();
    }

    public ArrayList<Vector3f> getVector3fs() {
        return vector3fs;
    }

    public ArrayList<Integer> getIndices() {
        return indices;
    }

    public ArrayList<Vector3f> getTextureCoordinates() {
        return texCoords;
    }
    public  ArrayList<ColorRGBA> getColors(){
        return colors;
    }
}