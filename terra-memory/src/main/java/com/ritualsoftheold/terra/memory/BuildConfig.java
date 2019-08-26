package com.ritualsoftheold.terra.memory;

/**
 * JVM-level settings for Terra.
 *
 */
public class BuildConfig {
    
    /**
     * Enables bound checking. This will make debugging easier, but may slightly
     * reduce performance. This is enabled by default.
     */
    public static final boolean CHECK_BOUNDS = System.getProperty("com.ritualsoftheold.terra.noBoundsChecks") != "true";
    
    /**
     * Checks bounds, provided that it is enabled (see {@link #CHECK_BOUNDS}.
     * @param addr Address to check against bounds.
     * @param useLength How long is the use (for example, 4 bytes for an int).
     * @param start Start of usable memory area.
     * @param length Length of usable memory area.
     * @return The address.
     * @throws IllegalAccessError When access would be out of bounds.
     */
    public static long inBounds(@Pointer long addr, long useLength, @Pointer long start, long length) {
        // Check if bounds checks are enabled at all
        if (!CHECK_BOUNDS) {
            return addr;
        }
        
        if (start == 0) {
            throw new IllegalAccessError("start cannot be NULL");
        }
        if (addr < start) {
            throw new IllegalAccessError("out of bounds (pos = " + addr + " exceeds max = " + (start + length));
        }
        if (addr + useLength > start + length) {
            throw new IllegalAccessError("out of bounds (pos = " + (addr + useLength) + " exceeds max = " + (start + length));
        }
        
        return addr;
    }
}
