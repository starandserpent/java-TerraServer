package com.ritualsoftheold.terra.data;

import com.ritualsoftheold.terra.Pointer;

/**
 * An object which somehow requires offheap data to be in place.
 *
 */
public interface OffheapObject {
    
    @Pointer
    long memoryAddress();
    
    int memoryLength();
}
