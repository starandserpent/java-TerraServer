package com.ritualsoftheold.terra.memory;

/**
 * Memory allocation helper. Might recycle memory, track it or just allocate
 * and do nothing more.
 *
 */
public interface MemoryAllocator {
    
    /**
     * Allocates memory for given amount of data.
     * @param length Length of data.
     * @return Memory address where to put it.
     */
    @Pointer long allocate(long length);
    
    /**
     * Frees memory. This is rather low level method;
     * be careful NOT to free data which may be used.
     * @param length Length of the data.
     */
    void free(@Pointer long addr, long length);
}
