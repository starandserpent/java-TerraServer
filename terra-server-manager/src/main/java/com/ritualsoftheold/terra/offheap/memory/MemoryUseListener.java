package com.ritualsoftheold.terra.offheap.memory;

/**
 * Listens to memory allocations and free's.
 *
 */
public interface MemoryUseListener {
    
    /**
     * Called when allocating memory.
     * @param amount How much was allocated.
     */
    void onAllocate(long amount);
    
    /**
     * Called when freeing memory.
     * @param amount How much was freed.
     */
    void onFree(long amount);
}
