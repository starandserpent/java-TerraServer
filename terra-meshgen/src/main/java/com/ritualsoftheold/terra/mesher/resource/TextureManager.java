package com.ritualsoftheold.terra.mesher.resource;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.jme3.asset.AssetManager;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.TextureArray;
import com.jme3.texture.image.ColorSpace;
import com.ritualsoftheold.terra.core.material.MaterialRegistry;
import com.ritualsoftheold.terra.core.material.TerraMaterial;
import com.ritualsoftheold.terra.core.material.TerraTexture;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;

/**
 * Manages textures of materials. Creates texture atlases.
 *
 */
public class TextureManager {

    private static final int TEXTURE_MIN_RES = 2;
    //Size of the cube
    private static final int ATLAS_SIZE = 256;
    private static final int BYTES_PER_PIXEL = 4;
    private int x;
    private int y;
    private ByteBuffer atlasBuf;

    private AssetManager assetManager;
    public TextureManager(AssetManager assetManager) {
        this.assetManager = assetManager;
        atlasBuf = ByteBuffer.allocateDirect(4099 * 4096 * BYTES_PER_PIXEL);
    }
    /**
     * Returns texture array used for ground texture.
     * @return Ground texture array.
     */

    public TextureArray convertTexture(TerraTexture[][][] terraTextures, TerraTexture mainTexture) {
        ArrayList<Image> atlases = new ArrayList<>();
        ByteBuffer atlasBuf = makeMainImage(mainTexture);

        int texturesPerSide = 16;
               // Assign texture data for shader
        mainTexture.setPage(1); // Texture array id, "page"
        mainTexture.setTileId(0); // Texture tile id
        mainTexture.setTexturesPerSide(16); // For MeshContainer


        for (int y = 0; y < 16; y += 1) {
            x = 0;
            this.y = y;
            for (int x = 0; x < 16; x += 1) {
                TerraTexture terraTexture = terraTextures[0][y][x];
                if (terraTexture != null && terraTexture != mainTexture) {
                    this.x = x;
                    Image image = assetManager.loadTexture(terraTexture.getAsset()).getImage();
                    makeTile(image, atlasBuf, 1);
                    terraTexture.setPage(2); // Texture array id, "page"
                    terraTexture.setTileId(0); // Texture tile id
                    terraTexture.setTexturesPerSide(16); // For MeshContainer
                }
            }
        }

        Image incompleteAtlas = new Image(Format.ABGR8, 1024, 1024, atlasBuf, null, ColorSpace.Linear);
        atlasBuf.clear();
        atlases.add(incompleteAtlas);

        TextureArray array = new TextureArray(atlases);
        array.setMagFilter(MagFilter.Nearest);
       array.setMinFilter(MinFilter.NearestNoMipMaps);

        return array;
    }

    public TextureArray convertMainTexture(TerraTexture mainTexture){
        ArrayList<Image> atlases = new ArrayList<>();

        ByteBuffer atlasBuf = makeMainImage(mainTexture);

        Image incompleteAtlas = new Image(Format.ABGR8, ATLAS_SIZE, ATLAS_SIZE, atlasBuf, null, ColorSpace.Linear);
        atlases.add(incompleteAtlas);
        atlasBuf.clear();

        TextureArray array = new TextureArray(atlases);
        array.setMagFilter(MagFilter.Nearest);
        array.setMinFilter(MinFilter.NearestNoMipMaps);

        return array;
    }

    private ByteBuffer makeMainImage(TerraTexture previousTexture) {
        Image image = assetManager.loadTexture(previousTexture.getAsset()).getImage();
        atlasBuf.clear();
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

    public int getAtlasSize() {
        return ATLAS_SIZE;
    }
}