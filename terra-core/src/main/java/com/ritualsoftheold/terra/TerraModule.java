package com.ritualsoftheold.terra;

import java.util.HashSet;
import java.util.Set;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.material.TerraMaterial;

public class TerraModule {
    
    private String uniqueId;
    
    private Set<TerraMaterial> materials;
    
    public TerraModule(String id) {
        this.uniqueId = id;
        this.materials = new HashSet<>();
    }
    
    /**
     * Gets unique identifier of this module.
     * @return Module id.
     */
    public String getUniqueId() {
        return uniqueId;
    }
    
    /**
     * Creates a material builder, applies this module to it and
     * then remembers that material so it can be correctly registered.
     * @return A material builder.
     */
    public TerraMaterial.Builder newMaterial() {
        TerraMaterial.Builder builder = TerraMaterial.builder().module(this);
        materials.add(builder.build());
        return builder;
    }
    
    public void registerMaterials(MaterialRegistry reg) {
        for (TerraMaterial mat : materials) {
            reg.registerMaterial(mat, this);
        }
    }
    
}
