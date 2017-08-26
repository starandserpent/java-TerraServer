package com.ritualsoftheold.terra.mesher.resource;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.jme3.asset.AssetManager;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture;
import com.jme3.texture.TextureArray;
import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.material.TerraMaterial;
import com.ritualsoftheold.terra.material.TerraTexture;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
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
    private static final int ATLAS_SIZE = 64;
    private static final int BYTES_PER_PIXEL = 4;
    private static final int ATLAS_SIZE_IMAGE = ATLAS_SIZE * BYTES_PER_PIXEL;
    
    private static final int IMAGE_UP_LEFT = ATLAS_SIZE * ATLAS_SIZE * BYTES_PER_PIXEL;
    
    private Short2ObjectMap<TerraTexture> textures;
    private AssetManager assetManager;
    
    private TextureArray array;
    private List<Image> atlases;
    private FloatList texCoords;
    
    public TextureManager(AssetManager assetManager) {
        textures = new Short2ObjectArrayMap<>();
        texCoords = new FloatArrayList();
        this.assetManager = assetManager;
    }
    
    /**
     * Returns texture array used for ground texture.
     * @return
     */
    public TextureArray getGroundTexture() {
        return array;
    }
    
    public Image getAtlas(int index) {
        return atlases.get(index);
    }
    
    public TerraTexture getTexture(short worldId) {
        return textures.get(worldId);
    }
    
    public void loadMaterials(MaterialRegistry reg) {
        textures.clear(); // Clear previous textures
        
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
        
        List<Image> atlases = new ArrayList<>(); // All texture atlases go here
        for (Entry<List<TerraTexture>> e : resulutions.int2ObjectEntrySet()) {
            generateAtlases(e.getValue(), e.getIntKey(), atlases); // Generate atlases...
        }
        
        array = new TextureArray(atlases);
        array.setMagFilter(MagFilter.Nearest); // We want blocky style
        this.atlases = atlases;
    }
    
    private void generateAtlases(List<TerraTexture> textures, int size, List<Image> atlases) {
        int texturesPerSide = ATLAS_SIZE / size;
        
        int x = 0;
        int y = 0;
        ByteBuffer atlasBuf = ByteBuffer.allocateDirect(ATLAS_SIZE * ATLAS_SIZE * BYTES_PER_PIXEL); // 4 for alpha channel+colors, TODO configurable
        for (TerraTexture texture : textures) {
            Image img = assetManager.loadTexture(texture.getAsset()).getImage(); // Use asset manager to load
            if (x == texturesPerSide) { // Pick next row
                x = 0;
                y++;
            } if (y == texturesPerSide) { // Out of y values... need next atlas
                Image readyAtlas = new Image(Format.ABGR8, ATLAS_SIZE, ATLAS_SIZE, atlasBuf, null, com.jme3.texture.image.ColorSpace.sRGB);
                atlases.add(readyAtlas);
                atlasBuf = ByteBuffer.allocateDirect(ATLAS_SIZE * ATLAS_SIZE * BYTES_PER_PIXEL);
            }
            
            int atlasStart = x * size * BYTES_PER_PIXEL + y * size * ATLAS_SIZE_IMAGE;
            System.out.println(atlasStart);
            
            ByteBuffer imgData = img.getData(0);
            for (int i = 0; i < size; i++) {
                byte[] row = new byte[size * BYTES_PER_PIXEL]; // Create array for one row of image data
                imgData.position(i * size * BYTES_PER_PIXEL);
                imgData.get(row); // Copy one row of data to array
                atlasBuf.position(atlasStart + i * ATLAS_SIZE_IMAGE); // Travel to correct point in atlas data
                atlasBuf.put(row); // Set a row of data to atlas
            }
            
            // Set correct texture coordinates
            // X,Y=X and Y planes, Z=texture array index
            texture.assignTexCoords(x * 1.0f * size / ATLAS_SIZE, y * 1.0f * size / ATLAS_SIZE, atlases.size());
            System.out.println("Assign texture coordinates: " + texture.getTexCoordX() + ", " + texture.getTexCoordY() + ", " + texture.getTexCoordZ() + " for " + texture.getAsset());
            
            // Assign shader texture coordinate indices (this saves some RAM with mesh generation)
            texture.setTexCoordIndex(texCoords.size());
            float texMinX = texture.getTexCoordX(); // 0,0
            float texMinY = texture.getTexCoordY();
            
            float texMaxX = texMinX + texture.getScale() * 0.25f * texture.getWidth() / ATLAS_SIZE; // 1,1
            float texMaxY = texMinY + texture.getScale() * 0.25f * texture.getHeight() / ATLAS_SIZE;
            
            float texArray = texture.getTexCoordZ();
            
            // 0,0
            texCoords.add(texMinX);
            texCoords.add(texMinY);
            texCoords.add(texArray);
            
            // 0,1
            texCoords.add(texMinX);
            texCoords.add(texMaxY);
            texCoords.add(texArray);
            
            // 1,1
            texCoords.add(texMaxX);
            texCoords.add(texMaxY);
            texCoords.add(texArray);
            
            // 1,0
            texCoords.add(texMaxX);
            texCoords.add(texMinY);
            texCoords.add(texArray);
            
            x++;
        }
        
        // Not full atlas, but not empty either
        if (atlasBuf.position() != 0) {
            System.out.println("Incomplete atlas");
            Image incompleteAtlas = new Image(Format.ABGR8, ATLAS_SIZE, ATLAS_SIZE, atlasBuf, null, com.jme3.texture.image.ColorSpace.sRGB);
            atlases.add(incompleteAtlas);
        }
    }

    public float getAtlasSize() {
        return ATLAS_SIZE;
    }
    
    public FloatList getTexCoords() {
        return new FloatArrayList(texCoords);
    }
    
}
