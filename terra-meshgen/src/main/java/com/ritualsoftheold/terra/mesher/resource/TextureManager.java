package com.ritualsoftheold.terra.mesher.resource;

import com.jme3.texture.Image;
import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.material.TerraMaterial;
import com.ritualsoftheold.terra.material.TerraTexture;

import it.unimi.dsi.fastutil.shorts.Short2ObjectArrayMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;

/**
 * Manages textures of materials. Creates texture atlases.
 *
 */
public class TextureManager {
    
    private Short2ObjectMap<TerraTexture> textures;
    
    public TextureManager() {
        textures = new Short2ObjectArrayMap<>();
    }
    
    public void loadMaterials(MaterialRegistry reg) {
        textures.clear(); // Clear previous textures
        
        for (TerraMaterial mat : reg.getAllMaterials()) {
            TerraTexture texture = mat.getTexture();
            textures.put(mat.getWorldId(), texture); // Put texture to map
            
            // TODO texture array of atlases building
        }
    }
    
}
