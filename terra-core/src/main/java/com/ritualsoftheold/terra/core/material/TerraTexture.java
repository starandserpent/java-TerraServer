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

    public TerraTexture(float scale, String asset) {
        this.asset = asset;
        this.scale = scale;
    }
    
    public TerraTexture(String asset) {
        this(DEFAULT_SCALE, asset);
    }

    public void setSize(int width, int height){
        this.width = width;
        this.height = height;
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

    @Override
    public String toString() {
        return "texture:" + asset;
    }
}
