package com.ritualsoftheold.terra.material;

/**
 * Represents a texture in texture atlas.
 *
 */
public class TerraTexture {
    
    private static final int DEFAULT_SCALE = 1;
    
    private int width;
    private int height;
    
    private int scale;
    
    private String asset;
    
    private int texCoordX, texCoordY, texCoordZ;
    
    public TerraTexture(int width, int height, int scale, String asset) {
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
     * Gets scale divider of this texture. Values more than 1 mean that it will
     * not fit to single 25cm cube face.
     * @return Scale divider of this texture.
     */
    public int getScaleDivider() {
        return scale;
    }
    
    /**
     * Gets texture asset. What this means depends on implementation.
     * @return Some sort of asset identifier, like file name.
     */
    public String getAsset() {
        return asset;
    }
    
    public void assignTexCoords(int x, int y, int z) {
        texCoordX = x;
        texCoordY = y;
        texCoordZ = z;
    }
    
    public int getTexCoordX() {
        return texCoordX;
    }
    
    public int getTexCoordY() {
        return texCoordY;
    }
    
    public int getTexCoordZ() {
        return texCoordZ;
    }
    
    @Override
    public String toString() {
        return "texture:" + asset;
    }
}
