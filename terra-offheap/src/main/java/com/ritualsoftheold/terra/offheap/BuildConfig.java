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
    
    public static long inBounds(long addr, @Pointer long start, long length) {
        // Check if bounds checks are enabled at all
        if (CHECK_BOUNDS) {
            return addr;
        }
        
        if (start == 0) {
            throw new IllegalAccessError("start cannot be NULL");
        }
        if (addr < start) {
            throw new IllegalAccessError("out of bounds (pos = " + addr + " exceeds max = " + (start + length));
        }
        if (addr > start + length) {
            throw new IllegalAccessError("out of bounds (pos = " + addr + " exceeds max = " + (start + length));
        }
        
        return addr;
    }
}
