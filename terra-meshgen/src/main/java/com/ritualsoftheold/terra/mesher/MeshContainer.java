

        package com.ritualsoftheold.terra.mesher;

        import com.ritualsoftheold.terra.core.Terra;
        import com.ritualsoftheold.terra.core.material.TerraMaterial;
        import com.ritualsoftheold.terra.core.material.TerraTexture;
        import io.netty.buffer.ByteBuf;
        import io.netty.buffer.ByteBufAllocator;

        import java.util.ArrayList;
        import java.util.HashMap;

        /**
 * Helps with building meshes for voxel data.
 *
 */
public class MeshContainer {

    private ByteBuf vertices;

    private ByteBuf indices;

    private ByteBuf texCoords;

    private ArrayList<TerraTexture> textures;

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
        textures = new ArrayList<>();
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
        int nX = (int) (x * 256);
        int nY = (int) (y * 256);
        float packed1 = Float.intBitsToFloat(nX << 16 | nY);
        texCoords.writeFloatLE(packed1);

        float packed2 = Float.intBitsToFloat(page << 24 | tile << 12 | texturesPerSide);
        texCoords.writeFloatLE(packed2);
    }

    public void addTexture(TerraTexture texture){
        this.textures.add(texture);
    }

    public ByteBuf getVertices() {
        return vertices;
    }

    public ArrayList<TerraTexture> getTextures(){
        return textures;
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