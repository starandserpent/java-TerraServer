package com.ritualsoftheold.terra.offheap.data;

import com.ritualsoftheold.terra.offheap.Pointer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer.Allocator;

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
    public @Pointer long alloc(int length);
    
    /**
     * Frees memory. This is rather low level method;
     * be careful to NOT free data which may be used.
     * @param address Memory address of the data.
     * @param length Length of the data.
     */
    public void free(@Pointer long addr, int length);
    
    /**
     * Creates a dummy allocator, which will try to use given address
     * if length of first allocation matches length given here.
     * @param address Address where there is free.
     * @param length Length of free space.
     */
    Allocator createDummy(@Pointer long addr, int length);
    
}
