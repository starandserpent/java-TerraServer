package com.ritualsoftheold.terra;

import java.util.HashSet;
import java.util.Set;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.material.TerraMaterial;

/**
 * Terra modules are used to register materials. After that has been done,
 * they can be registered to use said materials. Materials inside modules may
 * not have name conflicts, but every module is its own namespace. In future,
 * unregistering materials in a module might be supported.
 *
 */
public class TerraModule {
    
    /**
     * Our id.
     */
    private final String uniqueId;
    
    /**
     * Materials registered here.
     */
    private final Set<TerraMaterial> materials;
    
    /**
     * Whether this module has been registered yet or not.
     */
    private boolean registered = false;
    
    /**
     * Creates a new Terra module
     * @param id Unique module id.
     */
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
        if (registered) {
            throw new IllegalStateException("materials already registered");
        }
        TerraMaterial.Builder builder = TerraMaterial.builder().module(this);
        materials.add(builder.build());
        return builder;
    }
    
    /**
     * Registers materials this module has to a material registry.
     * No new materials may be added to this module after registration.
     * @param reg Material registry.
     */
    public void registerMaterials(MaterialRegistry reg) {
        for (TerraMaterial mat : materials) {
            reg.registerMaterial(mat, this);
        }
        registered = true; // Disallow future registrations
    }
    
}
