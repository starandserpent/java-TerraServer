package com.ritualsoftheold.terra.offheap;

public class ChunkUtils {
    
    private ChunkUtils() {}
    
    public static float distance(float x, float y, float z) {
        return x + y * 16 + z * 256;
    }
    
    /**
     * Some black magic to get 0.5m/0.25m block index from
     * coordinates relative to bigger block's center.
     * @param x
     * @param y
     * @param z
     * @return
     */
    public static int getSmallBlockIndex(float x, float y, float z) {
        if (x <= 0) {
            if (y <= 0) {
                if (z <= 0) {
                    return 0;
                } else {
                    return 4;
                }
            } else {
                if (z <= 0) {
                    return 2;
                } else {
                    return 6;
                }
            }
        } else {
            if (y <= 0) {
                if (z <= 0) {
                    return 1;
                } else {
                    return 5;
                }
            } else {
                if (z <= 0) {
                    return 3;
                } else {
                    return 7;
                }
            }
        }
    }
    
    public static int get025BlockIndex(float x, float y, float z) {
        return 0;
    }
}
