package com.ritualsoftheold.terra.material;

import com.ritualsoftheold.terra.TerraModule;

public interface MaterialRegistry {
    
    /**
     * Gets a material. If it doesn't exist, it is created.
     * @param mod Module for material.
     * @param name Name of material.
     * @return The material.
     */
    TerraMaterial getMaterial(TerraModule mod, String name);
    
    /**
     * Gets a material, based on its char id. If it doesn't exist, null
     * is returned.
     * @param id World id.
     * @return The material.
     */
    TerraMaterial getForWorldId(short id);
}
