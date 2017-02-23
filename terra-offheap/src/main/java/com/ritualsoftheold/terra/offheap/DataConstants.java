package com.ritualsoftheold.terra.offheap;

public class DataConstants {
    
    /**
     * Array data offset.
     */
    public static final int ARRAY_DATA = 16;
    
    public static final int OCTREE_NODE_SIZE = 4;
    
    public static final int OCTREE_SIZE = 1 + OCTREE_NODE_SIZE * 8;
    
    public static final int CHUNK_SCALE = 16;
    
    public static final float SMALLEST_BLOCK = 0.25f;
    
    public static final int CHUNK_POINTER_STORE = 7;
    
    public static final int BLOCK_SIZE_DATA = 4096 / 4;
    
    public static final int MATERIAL_ATLAS = 256 * 3;
    
    /**
     * Chunk static data size.
     * 1 byte: flags
     * 256 * 3 bytes: Material atlas
     * 1024 bytes: block size data
     * 3 bytes: block data length (might be 0)
     * 3 bytes: extra data length (might be 0)
     */
    public static final int CHUNK_STATIC = 1 + MATERIAL_ATLAS + BLOCK_SIZE_DATA + 3 + 3;
    
    /**
     * Chunk static data size.
     * 1 byte: flags
     * 1024 bytes: block size data
     * 3 bytes: block data length (might be 0)
     * 3 bytes: extra data length (might be 0)
     */
    public static final int CHUNK_STATIC_NOATLAS = 1 + BLOCK_SIZE_DATA + 3 + 3;
}
