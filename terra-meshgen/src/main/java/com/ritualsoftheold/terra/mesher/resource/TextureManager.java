package com.ritualsoftheold.terra.mesher.resource;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.jme3.asset.AssetManager;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.ritualsoftheold.terra.core.material.TerraTexture;

/**
 * Manages textures of materials. Creates texture atlases.
 */
public class TextureManager {

    //Size of the cube
    private static final int ATLAS_SIZE = 256;
    private static final int BYTES_PER_PIXEL = 4;
    private int x;
    private int y;
    private ByteBuffer atlasBuf;

    private AssetManager assetManager;
    public TextureManager(AssetManager assetManager) {
        this.assetManager = assetManager;
        atlasBuf = ByteBuffer.allocateDirect(ATLAS_SIZE * ATLAS_SIZE * BYTES_PER_PIXEL);
    }

    public Texture2D convertTexture(TerraTexture[][][] terraTextures, TerraTexture mainTexture) {
        ByteBuffer atlasBuf = makeMainImage(mainTexture);


        for (int y = 0; y < 64; y += 1) {
            x = 0;
            this.y = y;
            for (int x = 0; x < 64; x += 1) {
                TerraTexture terraTexture = terraTextures[0][y][x];
                if (terraTexture != null && terraTexture != mainTexture) {
                    this.x = x;
                    Image image = assetManager.loadTexture(terraTexture.getAsset()).getImage();
                    makeTile(image, atlasBuf,  16);
                }
            }
        }

        return createTexture(atlasBuf);
    }

    public Texture2D convertMainTexture(TerraTexture mainTexture){
        ByteBuffer atlasBuf = makeMainImage(mainTexture);

        return createTexture(atlasBuf);
    }

    private Texture2D createTexture(ByteBuffer atlasBuf){
        Image incompleteAtlas = new Image(Format.ABGR8, ATLAS_SIZE, ATLAS_SIZE, atlasBuf, null, ColorSpace.Linear);
        atlasBuf.clear();

        Texture2D texture2D = new Texture2D(incompleteAtlas);
        texture2D.setMagFilter(MagFilter.Nearest);
        texture2D.setMinFilter(MinFilter.NearestNoMipMaps);
        return texture2D;
    }

    private ByteBuffer makeMainImage(TerraTexture previousTexture) {
        Image image = assetManager.loadTexture(previousTexture.getAsset()).getImage();
        atlasBuf.clear();
        x = 0;
        y = 0;
        makeTile(image, atlasBuf, 256);
        return atlasBuf;
    }

    public void makeTile(Image image, ByteBuffer atlasBuf, int size) {
        int atlasStart = x  * size * BYTES_PER_PIXEL + y * size * ATLAS_SIZE * BYTES_PER_PIXEL;

        ByteBuffer imgData = image.getData(0);
        for (int i = 0; i < size; i++) {
            byte[] row = new byte[size * BYTES_PER_PIXEL]; // Create array for one row of image data
            imgData.position(i * size * BYTES_PER_PIXEL);
            imgData.get(row); // Copy one row of data to array
            atlasBuf.position(atlasStart + i * ATLAS_SIZE * BYTES_PER_PIXEL); // Travel to correct point in atlas data
            atlasBuf.put(row); // Set a row of data to atlas
        }
    }
}