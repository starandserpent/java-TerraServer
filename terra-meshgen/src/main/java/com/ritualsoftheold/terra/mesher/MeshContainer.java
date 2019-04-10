package com.ritualsoftheold.terra.mesher;

        import com.ritualsoftheold.terra.core.Terra;
        import com.ritualsoftheold.terra.core.material.TerraMaterial;
        import com.ritualsoftheold.terra.core.material.TerraTexture;
        import com.ritualsoftheold.terra.mesher.resource.TextureManager;
        import io.netty.buffer.ByteBuf;
        import io.netty.buffer.ByteBufAllocator;

        import java.util.ArrayList;
        import java.util.Arrays;
        import java.util.HashMap;
        import java.util.List;

/**
 * Helps with building meshes for voxel data.
 *
 */
public class MeshContainer {

    private ByteBuf vertices;

    private ByteBuf indices;

    private ByteBuf texCoords;

    private TerraTexture[][] textures;

    private HashMap<TerraTexture, Integer> textureTypes;

    /**
     * Creates a new mesh container.
     *
     * <p><strong>Remember to {@link #release()} this container!</strong>
     * Otherwise, it is easy to exhaust Netty's buffer allocation,
     * which might cause freezes during mesh generation.
     * @param startSize Starting number of vertices that the container may
     * hold. Buffers will get enlarged if this is exceeded.
     * @param allocator Buffer allocator.
     */
    public MeshContainer(int startSize, ByteBufAllocator allocator) {
        this.vertices = allocator.directBuffer(startSize * 4);
        this.indices = allocator.directBuffer(startSize * 6);
        this.texCoords = allocator.directBuffer(startSize * 4);
        textures = new TerraTexture[TextureManager.ATLAS_SIZE * 64][];
        textureTypes = new HashMap<>();
    }

    public void vertex(int x, int y, int z) {
        float packed = Float.intBitsToFloat(x << 22 | y << 12 | z);
        vertices.writeFloatLE(packed);
    }

    public void triangle(int vertIndex, int i, int j, int k) {
        indices.writeIntLE(vertIndex + i);
        indices.writeIntLE(vertIndex + j);
        indices.writeIntLE(vertIndex +  k);
    }

    public void texture(int page, int tile, int texturesPerSide, float x, float y) {
        int nX = (int) x * 256;
        int nY = (int) y * 256;

        float packed1 = Float.intBitsToFloat(nX << 16 | nY);
        texCoords.writeFloatLE(packed1);

        float packed2 = Float.intBitsToFloat(page << 24 | tile << 12 | texturesPerSide);
        texCoords.writeFloatLE(packed2);
    }

    public void setTextures(int nX, int nY, int nZ, TerraTexture texture) {
       // nY *= nZ;
        if (nZ == 1){
            System.out.println("fsdad");
        }
        for(int x2 = nX; x2 < 16 + nX; x2++){
            if(textures[x2] == null) {
                textures[x2] = new TerraTexture[TextureManager.ATLAS_SIZE * 64];
            }
            Arrays.fill(textures[x2], nY, 16 + nY, texture);
        }

        int i = 1;
        if(textureTypes.get(texture) != null) {
            i = textureTypes.get(texture);
            i++;
        }
        textureTypes.put(texture, i);
    }

    public ByteBuf getVertices() {
        return vertices;
    }

    public TerraTexture[][] getTextures(){
        return textures;
    }

    public TerraTexture getMainTexture() {
        int max = 0;
        TerraTexture texture = null;
        for (TerraTexture key : textureTypes.keySet()) {
            if(textureTypes.get(key) == 0) {
                textureTypes.remove(texture);
            } else if (textureTypes.get(key) > max) {
                max = textureTypes.get(key);
                texture = key;
            }
        }

        return texture;
    }

    public int getTextureTypes() {
        return textureTypes.size();
    }

    public ByteBuf getIndices() {
        return indices;
    }

    public ByteBuf getTextureCoordinates() {
        return texCoords;
    }

    /**
     * Releases the underlying Netty buffers. Once you don't use them anymore,
     * this should be called to ensure that Netty's buffer allocator is not
     * exhausted. Because you don't want mesh generator to freeze randomly
     * in future, do you?
     */
    public void release() {
        vertices.release();
        indices.release();
        texCoords.release();
    }
}