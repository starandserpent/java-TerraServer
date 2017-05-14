package com.ritualsoftheold.terra.mesher.resource;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.jme3.asset.AssetManager;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.TextureArray;
import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.material.TerraMaterial;
import com.ritualsoftheold.terra.material.TerraTexture;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.shorts.Short2ObjectArrayMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;

/**
 * Manages textures of materials. Creates texture atlases.
 *
 */
public class TextureManager {
    
    private static final int TEXTURE_MIN_RES = 2;
    private static final int ATLAS_SIZE = 4096;
    
    private Short2ObjectMap<TerraTexture> textures;
    private AssetManager assetManager;
    
    private TextureArray array;
    
    public TextureManager(AssetManager assetManager) {
        textures = new Short2ObjectArrayMap<>();
        this.assetManager = assetManager;
    }
    
    /**
     * Returns texture array used for ground texture.
     * @return
     */
    public TextureArray getGroundTexture() {
        return array;
    }
    
    public void loadMaterials(MaterialRegistry reg) {
        textures.clear(); // Clear previous textures
        
        Int2ObjectMap<List<TerraTexture>> resulutions = new Int2ObjectArrayMap<>();
        
        for (TerraMaterial mat : reg.getAllMaterials()) {
            TerraTexture texture = mat.getTexture();
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
        
        List<Image> atlases = new ArrayList<>(); // All texture atlases go here
        for (Entry<List<TerraTexture>> e : resulutions.int2ObjectEntrySet()) {
            generateAtlases(e.getValue(), e.getIntKey(), atlases); // Generate atlases...
        }
        
        array = new TextureArray(atlases);
    }
    
    private void generateAtlases(List<TerraTexture> textures, int size, List<Image> atlases) {
        int texturesPerSide = ATLAS_SIZE / size;
        
        int x = 0;
        int y = 0;
        ByteBuffer atlasBuf = ByteBuffer.allocateDirect(ATLAS_SIZE * ATLAS_SIZE * 4); // 4 for alpha channel+colors, TODO configurable
        for (TerraTexture texture : textures) {
            Image img = assetManager.loadTexture(texture.getAsset()).getImage(); // Use asset manager to load
            if (x == texturesPerSide) { // Pick next row
                x = 0;
                y++;
            } if (y == texturesPerSide) { // Out of y values... need next atlas
                Image readyAtlas = new Image(Format.ABGR8, ATLAS_SIZE, ATLAS_SIZE, atlasBuf, null, com.jme3.texture.image.ColorSpace.sRGB);
                atlases.add(readyAtlas);
                atlasBuf = ByteBuffer.allocateDirect(ATLAS_SIZE * ATLAS_SIZE * 4);
            }
            
            ByteBuffer imgData = img.getData(0);
            for (int i = 0; i < size; i++) {
                byte[] row = new byte[size]; // Create array for one row of image data
                imgData.get(row, i * size, size); // Copy one row of data to array
                atlasBuf.position(y * size * ATLAS_SIZE + x * size + i * ATLAS_SIZE); // Travel to correct point in atlas data
                atlasBuf.put(row); // Set a row of data to atlas
            }
            
            // Set correct texture coordinates
            // X,Y=X and Y planes, Z=texture array index
            texture.assignTexCoords(x * 1.0f * size / ATLAS_SIZE, y * 1.0f * size / ATLAS_SIZE, atlases.size());
        }
        
        // Not full atlas, but not empty either
        if (atlasBuf.position() != 0) {
            Image incompleteAtlas = new Image(Format.ABGR8, ATLAS_SIZE, ATLAS_SIZE, atlasBuf, null, com.jme3.texture.image.ColorSpace.sRGB);
            atlases.add(incompleteAtlas);
        }
    }
    
}
