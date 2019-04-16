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
    public DummyTextureManager(MaterialRegistry registry){
        dirt = registry.getMaterial("testgame:dirt");
        grass = registry.getMaterial("testgame:grass");
        air = registry.getMaterial("base:air");
    }

    public Texture2D convertTexture(AssetManager assetManager) {
        atlasBuf = ByteBuffer.allocateDirect(256 * 256 * 4);

        ByteBuffer imgData = assetManager.loadTexture(dirt.getTexture().getAsset()).getImage().getData(0);
        for (int i = 0; i < 256; i++) {
            byte[] row = new byte[256 * 4]; // Create array for one row of image data
            imgData.position(i * 256 * 4);
            imgData.get(row); // Copy one row of data to array
            atlasBuf.position(i * 256 * 4); // Travel to correct point in atlas data
            atlasBuf.put(row); // Set a row of data to atlas
        }

        int atlasStart = 4  * 64 * 4;
        imgData.clear();
        imgData = assetManager.loadTexture(grass.getTexture().getAsset()).getImage().getData(0);
        for (int i = 0; i < 64; i++) {
            byte[] row = new byte[64 * 4]; // Create array for one row of image data
            imgData.position(i * 64 * 4);
            imgData.get(row); // Copy one row of data to array
            atlasBuf.position(atlasStart+ i * 256 * 4); // Travel to correct point in atlas data
            atlasBuf.put(row); // Set a row of data to atlas
        }

        atlasStart = 4  * 64 * 4 + 4 * 256 *4;
        for (int i = 0; i < 64; i++) {
            byte[] row = new byte[64 * 4]; // Create array for one row of image data
            imgData.position(i * 64 * 4);
            imgData.get(row); // Copy one row of data to array
            atlasBuf.position(atlasStart+ i * 256 * 4); // Travel to correct point in atlas data
            atlasBuf.put(row); // Set a row of data to atlas
        }

        atlasStart = 4  * 256 * 4;
        for (int i = 0; i < 64; i++) {
            byte[] row = new byte[64 * 4]; // Create array for one row of image data
            imgData.position(i * 64 * 4);
            imgData.get(row); // Copy one row of data to array
            atlasBuf.position(atlasStart+ i * 256 * 4); // Travel to correct point in atlas data
            atlasBuf.put(row); // Set a row of data to atlas
        }


        Image incompleteAtlas = new Image(Image.Format.ABGR8, 256, 256, atlasBuf, null, ColorSpace.Linear);

        return new Texture2D(incompleteAtlas);
    }
}
