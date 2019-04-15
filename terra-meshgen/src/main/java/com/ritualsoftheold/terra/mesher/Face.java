package com.ritualsoftheold.terra.mesher;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

public class Face {
    private Vector2f[] textureCoords;
    private Vector3f[] vector3f;
    private int[] indexes;

    public Face(){
        textureCoords = new Vector2f[4];
        vector3f = new Vector3f[4];
        indexes = new int[6];
    }

    public void setTextureCoords(int x, int y, int position) {
        this.textureCoords[position] = new Vector2f(x, y);
    }

    public void setVector3f(int x, int y, int z, int position) {
        this.vector3f[position] = new Vector3f(x/16f, y/16f, z/16f);
    }

    public void setVector3f(Vector3f vector3f, int position) {
        this.vector3f[position] = vector3f;
    }


    public void setIndexes(int indexes, int position) {
        this.indexes[position] = indexes;
    }

    public Vector2f[] getTextureCoords() {
        return textureCoords;
    }

    public int[] getIndexes() {
        return indexes;
    }

    public Vector3f[] getVector3f() {
        return vector3f;
    }
}
