package com.ritualsoftheold.terra.offheap;

public class DataConstants {
    
    /**
     * Array data offset (JVM stuff).
     */
    public static final int ARRAY_DATA = 16;
    
    public static final int OCTREE_NODE_SIZE = 4;
    
    public static final int OCTREE_SIZE = 1 + OCTREE_NODE_SIZE * 8;
    
    public static final int CHUNK_SCALE = 16;
    
    public static final int CHUNK_COORD_X = 1, CHUNK_COORD_Y = 16, CHUNK_COORD_Z = 256;
    
    public static final float SMALLEST_BLOCK = 0.25f;
    
    public static final int CHUNK_POINTER_STORE = 7;
    
    public static final int BLOCK_SIZE_DATA = 4096 / 4;
    
    public static final int CHUNK_MAX_BLOCKS = DataConstants.CHUNK_SCALE * DataConstants.CHUNK_SCALE * DataConstants.CHUNK_SCALE * 64;
    
    public static final int CHUNK_MIN_BLOCKS = DataConstants.CHUNK_SCALE * DataConstants.CHUNK_SCALE * DataConstants.CHUNK_SCALE;

    /**
     * Material id length (in bytes).
     */
    public static final int MATERIAL_LENGTH = 2;
    
    public static final int CHUNK_UNCOMPRESSED = CHUNK_MAX_BLOCKS * MATERIAL_LENGTH;
    
    public static final int CHUNK_DATA_OFFSET = 1;
    
    /**
     * Octree group metadata length.
     */
    public static final int OCTREE_GROUP_META = 24;
    
}
