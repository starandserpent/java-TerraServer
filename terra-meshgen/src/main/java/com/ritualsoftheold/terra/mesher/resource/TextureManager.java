package com.ritualsoftheold.terra.mesher.resource;

import com.jme3.asset.AssetManager;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.TextureArray;
import com.ritualsoftheold.terra.core.material.MaterialRegistry;
import com.ritualsoftheold.terra.core.material.TerraMaterial;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Manages textures of materials. Creates texture atlases.
 */
public class TextureManager{

    private int maxHeight;
    private int maxWidth;
    private static final Image.Format DEFAULT_IMAGE_FORMAT = Image.Format.ABGR8;
    private static final int BYTES_PER_PIXEL = 4;

    //Size of the cube
    private TextureArray textureArray;

    public TextureManager(AssetManager assetManager, MaterialRegistry registry) {
        ArrayList<Image> atlas = new ArrayList<>();

        for (TerraMaterial material : registry.getAllMaterials()) {
            if (material.getTexture() != null) {
                Texture tex = assetManager.loadTexture(material.getTexture().getAsset());
//                tex.setMagFilter(Texture.MagFilter.Nearest);
//                tex.setMinFilter(Texture.MinFilter.NearestNearestMipMap);
                Image image = tex.getImage();

                if (image.getHeight() > maxHeight) {
                    maxHeight = image.getHeight();
                }

                if (image.getWidth() > maxWidth) {
                    maxWidth = image.getWidth();
                }
                atlas.add(image);
            }
        }

        for (TerraMaterial material : registry.getAllMaterials()) {
            if (material.getTexture() != null) {
                material.getTexture().setSize(maxWidth, maxHeight);
            }
        }

        for (Image image:atlas) {
            image.setFormat(DEFAULT_IMAGE_FORMAT);
            if (image.getWidth() < maxWidth || image.getHeight() < maxHeight) {
                int size = Math.max(image.getWidth(), image.getHeight());
                completeImages(image, size);
            }
        }

        textureArray = new TextureArray(atlas);
    }

    private void completeImages(Image image, int size) {
        int atlasSize = Math.max(maxHeight, maxWidth);
        int atlasSizeImage = atlasSize * BYTES_PER_PIXEL;

        ByteBuffer atlasBuf = ByteBuffer.allocateDirect(atlasSize * atlasSize * BYTES_PER_PIXEL); // 4 for alpha channel+colors, TODO configurable
        ByteBuffer imgData = image.getData(0);

        for(int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                int atlasStart = x * size * BYTES_PER_PIXEL + y * size * atlasSizeImage;

                for (int i = 0; i < size; i++) {
                    byte[] row = new byte[size * BYTES_PER_PIXEL]; // Create array for one row of image data
                    imgData.position(i * size * BYTES_PER_PIXEL);
                    imgData.get(row); // Copy one row of data to array
                    atlasBuf.position(atlasStart + i * atlasSizeImage); // Travel to correct point in atlas data
                    atlasBuf.put(row); // Set a row of data to atlas
                }
            }
        }

        image.setHeight(maxHeight);
        image.setWidth(maxWidth);
        image.setData(atlasBuf);
    }

    public TextureArray getTextureArray() {
        return textureArray;
    }
}