package com.ritualsoftheold.terra.core.material;

public class TerraMesh {

    private String asset;

    private int defaultDistanceX;
    private int defaultDistanceY;
    private int defaultDistanceZ;

    private byte id;

    public TerraMesh(String asset, float defaultDistanceX, float defaultDistanceY, float defaultDistanceZ) {
        this.asset = asset;

        this.defaultDistanceX = (int) (defaultDistanceX);
        this.defaultDistanceY = (int) (defaultDistanceY);
        this.defaultDistanceZ = (int) (defaultDistanceZ);
    }

    /**
     * Gets texture asset. What this means depends on implementation.
     * @return Some sort of asset identifier, like file name.
     */
    public String getAsset() {
        return asset;
    }

    void setVoxelId(byte id) {
        this.id = id;
    }

    public int getSize(){
        return (defaultDistanceX * defaultDistanceY * defaultDistanceZ);
    }

    public int getDefaultDistanceX() {
        return defaultDistanceX;
    }

    public int getDefaultDistanceY() {
        return defaultDistanceY;
    }

    public int getDefaultDistanceZ() {
        return defaultDistanceZ;
    }

    public byte getId() {
        return id;
    }

    @Override
    public String toString() {
        return "model:" + asset;
    }
}
