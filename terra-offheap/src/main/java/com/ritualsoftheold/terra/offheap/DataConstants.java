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
}
