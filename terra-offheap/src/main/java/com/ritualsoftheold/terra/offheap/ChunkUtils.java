package com.ritualsoftheold.terra.offheap;

public class ChunkUtils {
    
    private ChunkUtils() {}
    
    public static float distance(float x, float y, float z) {
        return x + y * 16 + z * 256;
    }
}
