package com.ritualsoftheold.terra.world.test;

import com.jme3.asset.AssetManager;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.TextureArray;
import com.jme3.texture.image.ColorSpace;
import com.ritualsoftheold.terra.core.material.MaterialRegistry;
import com.ritualsoftheold.terra.core.material.TerraMaterial;
import com.ritualsoftheold.terra.core.material.TerraTexture;

import java.nio.ByteBuffer;
import java.util.ArrayList;


public class DummyTextureManager{
    private TerraMaterial dirt;
    private TerraMaterial air;
    private TerraMaterial grass;
    private ByteBuffer atlasBuf;

    int ATLAS_SIZE = 64;
    int size;

    public DummyTextureManager(MaterialRegistry registry){
        dirt = registry.getMaterial("testgame:dirt");
        grass = registry.getMaterial("testgame:grass");
        air = registry.getMaterial("base:air");
        atlasBuf = ByteBuffer.allocateDirect(ATLAS_SIZE * ATLAS_SIZE * 4);
    }

    public Texture2D convertTexture(AssetManager assetManager, TerraTexture mainTexture) {

        ByteBuffer imgData = assetManager.loadTexture(dirt.getTexture().getAsset()).getImage().getData(0);
        size = 64;
        setAtlasBuf(imgData, 0);

        size = 16;
        imgData = assetManager.loadTexture(grass.getTexture().getAsset()).getImage().getData(0);
        setAtlasBuf(imgData, 0);

        Image incompleteAtlas = new Image(Image.Format.ABGR8, ATLAS_SIZE, ATLAS_SIZE, atlasBuf, null, ColorSpace.Linear);

        return new Texture2D(incompleteAtlas);
    }

    private void setAtlasBuf(ByteBuffer imgData, int atlasStart){
        for (int i = 0; i < size; i++) {
            byte[] row = new byte[size * 4]; // Create array for one row of image data
            imgData.position(i * size * 4);
            imgData.get(row); // Copy one row of data to array
            atlasBuf.position(atlasStart+ i * ATLAS_SIZE * 4); // Travel to correct point in atlas data
            atlasBuf.put(row); // Set a row of data to atlas
        }

    }
}
