package com.ritualsoftheold.terra.material;

/**
 * Represents a texture in texture atlas.
 *
 */
public interface TerraTexture {
    
    /**
     * Gets height of the texture.
     * @return Height of texture.
     */
    int getWidth();
    
    /**
     * Gets width of texture.
     * @return Width of texture.
     */
    int getHeight();
    
    /**
     * Gets scale of this texture. In the other words,
     * this big amount of the texture will go to 0.25m square meter area.
     * Values fall in range of [0, 1[.
     * @return Scale of the texture.
     */
    float getScale();
    
    /**
     * Gets texture asset. What this means depends on implementation.
     * @return Some sort of asset identifier, like file name.
     */
    String getAsset();
    
    void assignTexCoords(float x, float y, float z);
    
    float getTexCoordX();
    
    float getTexCoordY();
    
    float getTexCoordZ();
}
