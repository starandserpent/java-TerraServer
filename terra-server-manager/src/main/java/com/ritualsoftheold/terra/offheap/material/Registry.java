package com.ritualsoftheold.terra.offheap.material;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class Registry {
    
    // Preferred ids for materials
    private List<String> preferredIds;
    
    private Int2ObjectMap<TerraObject> idToObject;
    private Object2ObjectMap<String, TerraObject> nameToObject;
    
    public Registry(List<String> preferredIds) {
        this.preferredIds = new ArrayList<>(preferredIds);
        idToObject = new Int2ObjectArrayMap<>(preferredIds.size());
        nameToObject = new Object2ObjectOpenHashMap<>(preferredIds.size());
        registerDefaultMaterials();
    }
    
    public Registry() {
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
    public TerraObject getMaterial(TerraModule mod, String name) {
        TerraObject material = nameToObject.get(mod.getUniqueId() + ":" + name);
        if (material == null) {
            throw new IllegalArgumentException("material not found");
        }
        return material;
    }
    
    public TerraObject getMaterial(String fullName) {
        TerraObject material = nameToObject.get(fullName);
        if (material == null) {
            throw new IllegalArgumentException("material not found");
        }
        return material;
    }
    
    /**
     * Attempts to register a material. If it has been already registered,
     * nothing will happen. The material will be assigned world id (short),
     * which can then be retrieved with {@link TerraObject#getWorldId()}.
     * How the ids will be assigned depends on implementation.
     * @param material Material to register.
     * @param mod Module which owns the material.
     */
    void registerMaterial(TerraObject material, TerraModule mod) {
        String fullName = mod.getUniqueId() + ":" + material.getName();
        material.setFullName(fullName);
        
        // Lookup for previous ids first
        int worldId = preferredIds.indexOf(fullName);
        if (worldId == - 1) { // Oops, not found
            worldId = preferredIds.size(); // Take next id...
            preferredIds.add(fullName); // ... and assign this here
        }

        worldId++;

        // Finally, assign world id to material
        material.setWorldId(worldId);
        
        // Put it to few other maps for ease of use
        nameToObject.put(fullName, material);
        idToObject.put((short) worldId, material);
        System.out.println(fullName + ": " + worldId);
    }
    
    /**
     * Gets a material, based on its char id. If it doesn't exist, null
     * is returned.
     * @param id World id.
     * @return The material.
     */
    public TerraObject getForWorldId(int id) {
        return idToObject.get(id);
    }
    
    public Collection<TerraObject> getAllMaterials() {
        return Collections.unmodifiableCollection(nameToObject.values()); // Just take the values, probably works well enough
    }
    
    public List<String> getPreferredIds() {
        return Collections.unmodifiableList(preferredIds);
    }
}
