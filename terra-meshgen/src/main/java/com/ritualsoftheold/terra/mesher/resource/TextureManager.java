package com.ritualsoftheold.terra.mesher.resource;

import com.jme3.asset.AssetManager;
import com.jme3.texture.Image;
import com.jme3.texture.TextureArray;
import com.ritualsoftheold.terra.core.material.MaterialRegistry;
import com.ritualsoftheold.terra.core.material.TerraMaterial;

import java.util.ArrayList;

/**
 * Manages textures of materials. Creates texture atlases.
 */
public class TextureManager {

    //Size of the cube
    public TextureArray textureArray;
    public TextureManager(AssetManager assetManager, MaterialRegistry registry) {
        ArrayList<Image> atlas = new ArrayList<>();
        for (TerraMaterial material : registry.getAllMaterials()) {
            if(material.getTexture() != null) {
                Image image = assetManager.loadTexture(material.getTexture().getAsset()).getImage();
                atlas.add(image);
            }
        }
        textureArray = new TextureArray(atlas);
    }

    public TextureArray getTextureArray() {
        return textureArray;
    }
}