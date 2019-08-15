package com.ritualsoftheold.terra.core.material;

import org.apache.commons.collections4.map.MultiKeyMap;

import java.util.Arrays;

public class TerraMesh {

    private String asset;
    private MultiKeyMap<Integer, byte[][][]> voxels;
    private int size;

    private float defaultDistanceX;
    private float defaultDistanceY;
    private float defaultDistanceZ;

    public TerraMesh(String asset, MultiKeyMap<Integer, byte[][][]> voxels, float defaultDistanceX,
                     float defaultDistanceY, float defaultDistanceZ) {
        this.asset = asset;
        this.voxels = voxels;

        this.defaultDistanceX = defaultDistanceX;
        this.defaultDistanceY = defaultDistanceY;
        this.defaultDistanceZ = defaultDistanceZ;

        size = 0;
        for (byte[][][] bounds : voxels.values()) {
            for (byte[][] bound : bounds) {
                for (byte[] bytes : bound) {
                    size += bytes.length;
                }
            }
        }
    }

    /**
     * Gets texture asset. What this means depends on implementation.
     * @return Some sort of asset identifier, like file name.
     */
    public String getAsset() {
        return asset;
    }

    void setVoxelId(byte id) {
        for(byte[][][] bounds : voxels.values()) {
            for (byte[][] bound : bounds) {
                for (byte[] bytes : bound) {
                    Arrays.fill(bytes, id);
                }
            }
        }
    }

    public MultiKeyMap<Integer, byte[][][]> getVoxels() {
        return voxels;
    }

    public int getSize() {
        return size;
    }

    public float getDefaultDistanceX() {
        return defaultDistanceX;
    }

    public float getDefaultDistanceY() {
        return defaultDistanceY;
    }

    public float getDefaultDistanceZ() {
        return defaultDistanceZ;
    }

    @Override
    public String toString() {
        return "model:" + asset;
    }
}
