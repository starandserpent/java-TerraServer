package com.ritualsoftheold.terra.offheap.data;

/**
 * An object which somehow requires offheap data to be in place.
 *
 */
public interface OffheapObject {
    
    long memoryAddress();
    
    void memoryAddress(long addr);
    
    boolean isValid();
    
    /**
     * Makes this object invalid, until it gets a memory address.
     */
    void invalidate();
}
