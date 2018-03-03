package com.ritualsoftheold.terra.mesher;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

/**
 * Helps with building meshes for voxel data.
 *
 */
public class MeshContainer {
    
    private ByteBuf vertices;
    
    private ByteBuf indices;
    
    private ByteBuf texCoords;
    
    /**
     * Creates a new mesh container.
     * @param startSize Starting number of vertices that the container may
     * hold. Buffers will get enlarged if this is exceeded.
     * @param allocator Buffer allocator.
     */
    public MeshContainer(int startSize, ByteBufAllocator allocator) {
        this.vertices = allocator.directBuffer(startSize * 4);
        this.indices = allocator.directBuffer(startSize * 6);
        this.texCoords = allocator.directBuffer(startSize * 4);
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
    
    public ByteBuf getVertices() {
        return vertices;
    }
    
    public ByteBuf getIndices() {
        return indices;
    }
    
    public ByteBuf getTextureCoordinates() {
        return texCoords;
    }
    
    /**
     * Releases the underlying Netty buffers. Make sure you have no NIO
     * buffers that refer to them. Failure to do so will likely result
     * a JVM segfault or other nasty behavior.
     */
    public void release() {
        vertices.release();
        indices.release();
        texCoords.release();
    }
}
