package com.ritualsoftheold.terra.offheap;

/**
 * Various constants about this build. Mainly used to have javac remove
 * if (CONSTANT) { ... } when said constant is false.
 *
 */
public class BuildConfig {
    
    /**
     * Enables bound checking. This will make debugging easier, but comes with
     * a performance penalty.
     */
    public static final boolean CHECK_BOUNDS = true;
    
    public static long inBounds(long pos, @Pointer long start, long length) {
        // Check if bounds checks are enabled at all
        if (CHECK_BOUNDS) {
            return pos;
        }
        
        if (start == 0) {
            throw new IllegalAccessError("start cannot be NULL");
        }
        if (pos < 0) {
            throw new IllegalAccessError("pos cannot be negative");
        }
        if (start + pos > start + length) {
            throw new IllegalAccessError("out of bounds (pos = " + pos + " exceeds max = " + (start + length));
        }
        
        return pos;
    }
}
