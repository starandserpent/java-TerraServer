package com.ritualsoftheold.terra.manager.data;

import com.ritualsoftheold.terra.manager.Pointer;

/**
 * An object which somehow requires offheap data to be in place.
 *
 */
public interface OffheapObject {
    
    @Pointer
    long memoryAddress();
    
    int memoryLength();
}
