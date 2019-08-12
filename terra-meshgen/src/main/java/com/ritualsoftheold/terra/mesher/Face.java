package com.ritualsoftheold.terra.mesher;

import com.jme3.math.Vector3f;
import com.ritualsoftheold.terra.core.material.TerraObject;
import org.jetbrains.annotations.NotNull;

public class Face implements Comparable<Face> {
    private Vector3f[] textureCoords;
    private Vector3f[] vector3f;
    private Vector3f[] normals;
    private TerraObject terraObject;

    Face() {
        textureCoords = new Vector3f[4];
        vector3f = new Vector3f[4];
        normals = new Vector3f[4];
    }

    void setNormals(Vector3f... normals) {
        this.normals = normals;
    }

    void setVector3f(int x, int y, int z, int position) {
        this.vector3f[position] = new Vector3f(x/4f, y/4f, z/4f);
    }

    void setVector3f(Vector3f vector3f, int position) {
        this.vector3f[position] = vector3f;
    }

    void setTextureCoords(float x, float y, int position) {
        this.textureCoords[position] = new Vector3f(x, y, terraObject.getTexture().getPosition());
    }

    public Vector3f[] getTextureCoords() {
        return textureCoords;
    }

    public Vector3f[] getVector3fs() {
        return vector3f;
    }

    void setObject(TerraObject terraObject) {
        this.terraObject = terraObject;
    }

    public TerraObject getObject() {
        return terraObject;
    }

    public Vector3f[] getNormals() {
        return normals;
    }


    @Override
    public int compareTo(@NotNull Face o) {
        if(o.vector3f[0].x < vector3f[0].x){
            return 1;
        }else if (o.vector3f[0].x > vector3f[0].x){
            return -1;
        }

        if(o.vector3f[0].y < vector3f[0].y){
            return 1;
        }else if (o.vector3f[0].y > vector3f[0].y){
            return -1;
        }

        if(o.vector3f[0].z < vector3f[0].z){
            return 1;
        }else if (o.vector3f[0].z > vector3f[0].z){
            return -1;
        }

        return 0;
    }
}
