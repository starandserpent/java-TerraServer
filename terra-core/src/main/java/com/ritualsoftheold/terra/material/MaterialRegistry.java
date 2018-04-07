package com.ritualsoftheold.terra.material;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.ritualsoftheold.terra.TerraModule;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectArrayMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;

public class MaterialRegistry {
    
    // Preferred ids for materials
    private List<String> preferredIds;
    
    private Int2ObjectMap<TerraMaterial> idToMaterial;
    private Object2ObjectMap<String, TerraMaterial> nameToMaterial;
    
    public MaterialRegistry(List<String> preferredIds) {
        this.preferredIds = new ArrayList<>(preferredIds);
        idToMaterial = new Int2ObjectArrayMap<>(preferredIds.size());
        nameToMaterial = new Object2ObjectOpenHashMap<>(preferredIds.size());
        registerDefaultMaterials();
    }
    
    public MaterialRegistry() {
        this(new ArrayList<>());
    }
    
    /**
     * Registers materials which are mandatory for Terra to function.
     */
    private void registerDefaultMaterials() {
        TerraModule mod = new TerraModule("base");
        registerMaterial(mod.newMaterial().name("air").build(), mod);
    }

    /**
     * Gets a material. If it doesn't exist, will return null.
     * @param mod Module for material.
     * @param name Name of material.
     * @return The material.
     */
    public TerraMaterial getMaterial(TerraModule mod, String name) {
        return nameToMaterial.get(mod.getUniqueId() + ":" + name);
    }
    
    public TerraMaterial getMaterial(String fullName) {
        return nameToMaterial.get(fullName);
    }
    
    /**
     * Attempts to register a material. If it has been already registered,
     * nothing will happen. The material will be assigned world id (short),
     * which can then be retrieved with {@link TerraMaterial#getWorldId()}.
     * How the ids will be assigned depends on implementation.
     * @param material Material to register.
     * @param mod Module which owns the material.
     */
    public void registerMaterial(TerraMaterial material, TerraModule mod) {
        String fullName = mod.getUniqueId() + ":" + material.getName();
        material.setFullName(fullName);
        
        // Lookup for previous ids first
        int worldId = preferredIds.indexOf(fullName);
        if (worldId == -1) { // Oops, not found
            worldId = preferredIds.size(); // Take next id...
            preferredIds.add(fullName); // ... and assign this here
        }
        
        // Finally, assign world id to material
        material.setWorldId(worldId);
        
        // Put it to few other maps for ease of use
        nameToMaterial.put(fullName, material);
        idToMaterial.put((short) worldId, material);
        System.out.println(fullName + ": " + worldId);
    }
    
    /**
     * Gets a material, based on its char id. If it doesn't exist, null
     * is returned.
     * @param id World id.
     * @return The material.
     */
    public TerraMaterial getForWorldId(int id) {
        return idToMaterial.get(id);
    }
    
    public Collection<TerraMaterial> getAllMaterials() {
        return Collections.unmodifiableCollection(idToMaterial.values()); // Just take the values, probably works well enough
    }
    
    public List<String> getPreferredIds() {
        return Collections.unmodifiableList(preferredIds);
    }
}
