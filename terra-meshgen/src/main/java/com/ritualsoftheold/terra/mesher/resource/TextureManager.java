package com.ritualsoftheold.terra.mesher.resource;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.jme3.asset.AssetManager;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.TextureArray;
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
    private int atlasSize;
    private static final int BYTES_PER_PIXEL = 4;
    private int x;
    private int y;

    private Int2ObjectMap<TerraTexture> textures;
    private AssetManager assetManager;
    private MaterialRegistry reg;

    private ArrayList<Image> atlases;

    public TextureManager(AssetManager assetManager, MaterialRegistry reg) {
        textures = new Int2ObjectArrayMap<>();
        atlases = new ArrayList<>();
        this.assetManager = assetManager;
        this.reg = reg;
    }
    /**
     * Returns texture array used for ground texture.
     * @return Ground texture array.
     */

    public TextureArray convertTexture(ArrayList<TerraTexture> terraTextures){
        // TODO make these configurable, Rituals art style already changed a bit since I wrote this
        x = 0;
        y = 0;
        atlasSize = 256 * terraTextures.size();
        atlases = new ArrayList<>();
        int texturesPerSide = atlasSize / 256;
        ByteBuffer atlasBuf = ByteBuffer.allocateDirect(atlasSize * atlasSize * BYTES_PER_PIXEL); //
        for(TerraTexture terraTexture: terraTextures) {
            atlasBuf = ByteBuffer.allocateDirect(atlasSize * atlasSize * BYTES_PER_PIXEL); // 4 for alpha channel+colors, TODO configurable
            Image image = assetManager.loadTexture(terraTexture.getAsset()).getImage();
            makeImage(image, terraTexture, texturesPerSide, atlasBuf, 256);
        }

        if (atlasBuf.position() != 0) {
            System.out.println("Incomplete atlas");
            Image incompleteAtlas = new Image(Format.ABGR8, atlasSize, atlasSize, atlasBuf, null, com.jme3.texture.image.ColorSpace.Linear);
            atlases.add(incompleteAtlas);
        }

        TextureArray array = new TextureArray(atlases);
        array.setMagFilter(MagFilter.Nearest);
        array.setMinFilter(MinFilter.NearestNoMipMaps);
        return array;
    }

    public void loadMaterials() {
        textures.clear(); // Clear previous textures

        atlases = new ArrayList<>();
        Int2ObjectMap<List<TerraTexture>> resulutions = new Int2ObjectArrayMap<>();

        for (TerraMaterial mat : reg.getAllMaterials()) {
            TerraTexture texture = mat.getTexture();
            if (texture == null) {
                continue; // This material has no texture (e.g. air)
            }
            textures.put(mat.getWorldId(), texture); // Put texture to map

            int width = texture.getWidth();
            int height = texture.getHeight();


            // TODO check that texture is power of 2
            if (width != height) {
                throw new UnsupportedOperationException("non-square textures are not yet supported");
            }

            List<TerraTexture> sameRes = resulutions.getOrDefault(width, new ArrayList<>()); // Get or create list of others with same res
            sameRes.add(texture); // Add this texture to list
            resulutions.put(width, sameRes); // Re-put list if we actually only just created it
        }

        atlasSize = 4096 * resulutions.int2ObjectEntrySet().size();
        for (Entry<List<TerraTexture>> e : resulutions.int2ObjectEntrySet()) {
            generateAtlases(e.getValue(), e.getIntKey()); // Generate atlases...
        }
    }

    private void generateAtlases(List<TerraTexture> textures, int size) {
        x = 0;
        y = 0;
        ByteBuffer atlasBuf = ByteBuffer.allocateDirect(atlasSize * atlasSize * BYTES_PER_PIXEL); // 4 for alpha channel+colors, TODO configurable
        int texturesPerSide = atlasSize / size;
        for (TerraTexture texture : textures) {
            Image img = assetManager.loadTexture(texture.getAsset()).getImage(); // Use asset manager to load
            makeImage(img, texture, texturesPerSide, atlasBuf, size);
        }

        // Not full atlas, but not empty either
        if (atlasBuf.position() != 0) {
            System.out.println("Incomplete atlas");
            Image incompleteAtlas = new Image(Format.ABGR8, atlasSize, atlasSize, atlasBuf, null, com.jme3.texture.image.ColorSpace.Linear);
            atlases.add(incompleteAtlas);
        }
    }

    public void makeImage(Image image, TerraTexture texture, int texturesPerSide, ByteBuffer atlasBuf, int size){
            if (x == texturesPerSide) { // Pick next row
                x = 0;
                y++;
            } if (y == texturesPerSide) { // Out of y values... need next atlas
                Image readyAtlas = new Image(Format.ABGR8, atlasSize, atlasSize, atlasBuf, null, com.jme3.texture.image.ColorSpace.Linear);
                atlases.add(readyAtlas);
                atlasBuf = ByteBuffer.allocateDirect(atlasSize * atlasSize * BYTES_PER_PIXEL);
            }

            int atlasStart = x * size * BYTES_PER_PIXEL + y * size *  atlasSize * BYTES_PER_PIXEL;

            ByteBuffer imgData = image.getData(0);
            for (int i = 0; i < size; i++) {
                byte[] row = new byte[size * BYTES_PER_PIXEL]; // Create array for one row of image data
                imgData.position(i * size * BYTES_PER_PIXEL);
                imgData.get(row); // Copy one row of data to array
                atlasBuf.position(atlasStart + i * atlasSize * BYTES_PER_PIXEL); // Travel to correct point in atlas data
                atlasBuf.put(row); // Set a row of data to atlas
            }

            // Assign texture data for shader
            texture.setPage(atlases.size()); // Texture array id, "page"
            texture.setTileId(y * texturesPerSide + x); // Texture tile id
            texture.setTexturesPerSide(texturesPerSide); // For MeshContainer
            x++;
    }

    public int getAtlasSize() {
        return atlasSize;
    }
}