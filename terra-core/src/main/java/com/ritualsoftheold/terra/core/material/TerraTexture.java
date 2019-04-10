package com.ritualsoftheold.terra.core.material;

/**
 * Represents a texture in texture atlas.
 *
 */
public class TerraTexture{
    
    private static final float DEFAULT_SCALE = 1;
    
    private int width;

    private int height;

    private float scale;
    
    private String asset;
    
    private int tileId;
    
    private int page;
    
    private int texturesPerSide;
    
    public TerraTexture(int width, int height, float scale, String asset) {
        this.width = width;
        this.height = height;
        this.asset = asset;
        this.scale = scale;
    }
    
    public TerraTexture(int width, int height, String asset) {
        this(width, height, DEFAULT_SCALE, asset);
    }
    
    /**
     * Gets height of the texture.
     * @return Height of texture.
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Gets width of texture.
     * @return Width of texture.
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Gets scale of this texture. In the other words,
     * this big amount of the texture will go to 0.25m square meter area.
     * Values fall in range of [0, 1[.
     * @return Scale of the texture.
     */
    public float getScale() {
        return scale;
    }
    
    /**
     * Gets texture asset. What this means depends on implementation.
     * @return Some sort of asset identifier, like file name.
     */
    public String getAsset() {
        return asset;
    }
    
    public int getTileId() {
        return tileId;
    }
    
    public void setTileId(int tileId) {
        this.tileId = tileId;
    }
    
    public int getPage() {
        return page;
    }
    
    public void setPage(int page) {
        this.page = page;
    }

    public int getTexturesPerSide() {
        return texturesPerSide;
    }

    public void setTexturesPerSide(int texturesPerPage) {
        this.texturesPerSide = texturesPerPage;
    }

    @Override
    public String toString() {
        return "texture:" + asset;
    }
}
