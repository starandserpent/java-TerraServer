package com.ritualsoftheold.terra.offheap.data;

import com.ritualsoftheold.terra.offheap.Pointer;

/**
 * An object which somehow requires offheap data to be in place.
 *
 */
public interface OffheapObject extends AutoCloseable {
    
    @Pointer long memoryAddress();
}
