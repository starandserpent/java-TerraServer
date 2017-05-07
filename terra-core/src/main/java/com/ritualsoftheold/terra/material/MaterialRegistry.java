package com.ritualsoftheold.terra.material;

import java.util.Collection;

import com.ritualsoftheold.terra.TerraModule;

public interface MaterialRegistry {
    
    /**
     * Gets a material. If it doesn't exist, will return a placeholder.
     * @param mod Module for material.
     * @param name Name of material.
     * @return The material.
     */
    TerraMaterial getMaterial(TerraModule mod, String name);
    
    /**
     * Attempts to register a material. If it has been already registered,
     * nothing will happen. The material will be assigned world id (short),
     * which can then be retrieved with {@link TerraMaterial#getWorldId()}.
     * How the ids will be assigned depends on implementation.
     * @param material Material to register.
     */
    void registerMaterial(TerraMaterial material);
    
    /**
     * Gets a material, based on its char id. If it doesn't exist, null
     * is returned.
     * @param id World id.
     * @return The material.
     */
    TerraMaterial getForWorldId(short id);
    
    Collection<TerraMaterial> getAllMaterials();
}
