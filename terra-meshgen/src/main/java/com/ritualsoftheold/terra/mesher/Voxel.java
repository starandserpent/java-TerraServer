package com.ritualsoftheold.terra.mesher;

import com.ritualsoftheold.terra.core.material.TerraMaterial;

public class Voxel {
    private Face[] faces;
    private TerraMaterial material;

    public Voxel(TerraMaterial material){
        faces = new Face[6];
        this.material = material;
    }

    public void setFace(Face face, int side){
        face.setMaterial(material);
        faces[side] = face;
    }

    public Face getFace(int side){
        return faces[side];
    }

    public TerraMaterial getTexture() {
        return material;
    }
}
