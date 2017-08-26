package com.ritualsoftheold.terra.material;

/**
 * Represents a texture in texture atlas.
 *
 */
public class TerraTexture {
    
    private static final float DEFAULT_SCALE = 4;
    
    private int width;
    private int height;
    
    private float scale;
    
    private String asset;
    
    private float texCoordX, texCoordY, texCoordZ;
    
    private int texCoordIndex;
    
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
    
    public void assignTexCoords(float x, float y, float z) {
        texCoordX = x;
        texCoordY = y;
        texCoordZ = z;
    }
    
    public float getTexCoordX() {
        return texCoordX;
    }
    
    public float getTexCoordY() {
        return texCoordY;
    }
    
    public float getTexCoordZ() {
        return texCoordZ;
    }
    
    @Override
    public String toString() {
        return "texture:" + asset;
    }

    public int getTexCoordIndex() {
        return texCoordIndex;
    }

    public void setTexCoordIndex(int texCoordIndex) {
        this.texCoordIndex = texCoordIndex;
    }
}
