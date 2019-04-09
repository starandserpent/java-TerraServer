package com.ritualsoftheold.terra.core.material;

import com.ritualsoftheold.terra.core.TerraModule;

/**
 * Represents a material of block.
 *
 */
public class TerraMaterial {
    
    /**
     * Constructs a new material builder.
     * @return Material builder.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    private TerraMaterial() {} // Only builder can create this
    
    /**
     * Id of this material in world data. Set by material registry. May vary
     * between different worlds!
     */
    private int worldId;
    
    /**
     * Full name of this material (namespace:material). Set by material
     * registry.
     */
    private String fullName;
    
    /**
     * Module this was originally registered to. Set by builder.
     */
    private TerraModule mod;
    
    /**
     * Name of this material without namespace. Set by builder.
     */
    private String name;
    
    /**
     * Texture definition. Set by builder.
     */
    private TerraTexture texture;
    
    /**
     * Gets this material's id in world data.
     * @return Material id.
     */
    public int getWorldId() {
        return worldId;
    }
    
    void setWorldId(int id) {
        worldId = id;
    }
    
    /**
     * Gets name of this material, without the namespace.
     * @return Material name.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets name of this material, including the namespace.
     * @return Full material name.
     */
    public String getFullName() {
        return fullName;
    }
    
    void setFullName(String name) {
        fullName = name;
    }
    
    /**
     * Gets texture definition associated with this material.
     * @return Texture definition or null.
     */
    public TerraTexture getTexture() {
        return texture;
    }
    
    /**
     * Builder for Terra's materials.
     *
     */
    public static class Builder {
        
        /**
         * Reference to material we are building right now.
         */
        private TerraMaterial material;
        
        private Builder() {
            material = new TerraMaterial();
        }
        
        /**
         * Sets module this material originates from. Usually set
         * automatically.
         * @param mod Module.
         * @return This builder.
         */
        public Builder module(TerraModule mod) {
            material.mod = mod;
            return this;
        }
        
        /**
         * Sets name of this material.
         * @param name Name of material.
         * @return This builder.
         */
        public Builder name(String name) {
            material.name = name;
            return this;
        }
        
        /**
         * Sets texture of this material.
         * @param texture Texture.
         * @return This builder.
         */
        public Builder texture(TerraTexture texture) {
            material.texture = texture;
            return this;
        }
        
        /**
         * Constructs a new Terra material.
         * @return Material.
         */
        public TerraMaterial build() {
            return material;
        }
    }
}
