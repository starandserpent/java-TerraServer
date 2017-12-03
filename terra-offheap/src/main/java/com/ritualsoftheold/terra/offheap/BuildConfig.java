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
    
    public static void checkBounds(@Pointer long start, long max, long pos, long length) {
        if (length == 0) {
            return; // No error possible, no data is read
        }
        if (start == 0) {
            throw new IllegalAccessError("start cannot be NULL");
        }
        if (pos < 0) {
            throw new IllegalAccessError("pos cannot be negative");
        }
        if (length < 0) {
            throw new IllegalAccessError("length cannot be negative");
        }
        if (start + pos > start + max) {
            throw new IllegalAccessError("out of bounds (pos = " + pos + " exceeds max = " + max);
        }
        if (start + pos + length > start + max) {
            throw new IllegalAccessError("out of bounds (pos + length = " + (pos + length) + " exceeds = " + max);
        }
    }
}
