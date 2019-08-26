package com.ritualsoftheold.terra.memory.data;


import com.ritualsoftheold.terra.memory.Pointer;

/**
 * An object which somehow requires offheap data to be in place.
 *
 */
public interface OffheapObject {
    
    @Pointer
    long memoryAddress();
    
    int memoryLength();
}
