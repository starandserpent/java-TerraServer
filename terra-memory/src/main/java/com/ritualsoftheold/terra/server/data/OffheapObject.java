package com.ritualsoftheold.terra.server.data;


import com.ritualsoftheold.terra.server.Pointer;

/**
 * An object which somehow requires offheap data to be in place.
 *
 */
public interface OffheapObject {
    
    @Pointer
    long memoryAddress();
    
    int memoryLength();
}
