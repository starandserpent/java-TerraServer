package com.ritualsoftheold.terra.core.material;

public class TerraMesh {

    private String asset;
    private boolean resource;

    public TerraMesh(String asset) {
        this(asset, false);
    }

    public TerraMesh(String asset, boolean resource) {
        this.asset = asset;
        this.resource = resource;
    }

    /**
     * Gets texture asset. What this means depends on implementation.
     * @return Some sort of asset identifier, like file name.
     */
    public String getAsset() {
        return asset;
    }

    public boolean isResource() {
        return resource;
    }

    @Override
    public String toString() {
        return "model:" + asset;
    }
}
