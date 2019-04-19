package com.ritualsoftheold.terra.mesher;

import com.ritualsoftheold.terra.core.material.TerraTexture;

public class Voxel {
    private Face[] faces;
    private TerraTexture texture;

    public Voxel(TerraTexture texture){
        faces = new Face[6];
        this.texture = texture;
    }

    public void setFace(Face face, int side){
        face.setTexture(texture);
        faces[side] = face;
    }

    public Face getFace(int side){
        return faces[side];
    }

    public TerraTexture getTexture() {
        return texture;
    }
}
