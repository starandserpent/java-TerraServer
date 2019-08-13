package com.ritualsoftheold.terra.core.material;

import java.util.Arrays;

public class TerraMesh {

    private String asset;
    private boolean resource;
    private byte[][][] voxels;

    public TerraMesh(String asset, boolean resource, byte[][][] voxels) {
        this.asset = asset;
        this.resource = resource;
        this.voxels = voxels;
    }

    /**
     * Gets texture asset. What this means depends on implementation.
     * @return Some sort of asset identifier, like file name.
     */
    public String getAsset() {
        return asset;
    }

    void setVoxelId(byte id) {
        for (int z = 0; z < voxels.length; z++){
            for (int y = 0; y < voxels[z].length; y++) {
                Arrays.fill(voxels[z][y], id);
            }
        }
    }

    public boolean isResource() {
        return resource;
    }

    public byte[][][] getVoxels() {
        return voxels;
    }

    public int getSizeInVoxels(){
        int lenght = 0;
        for (int z = 0; z < voxels.length; z++){
            for (int y = 0; y < voxels[z].length; y++) {
                lenght += voxels[z][y].length;
            }
        }
        return lenght;
    }

    public float getLenghtX(){
        return voxels[0][0].length * 0.25f;
    }

    public float getLenghtY(){
        return voxels[0].length * 0.25f;
    }

    public float getLenghtZ(){
        return voxels.length * 0.25f;
    }

    @Override
    public String toString() {
        return "model:" + asset;
    }
}
