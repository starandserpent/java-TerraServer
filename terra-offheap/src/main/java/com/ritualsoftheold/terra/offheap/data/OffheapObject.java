package com.ritualsoftheold.terra.offheap.data;

/**
 * An object which somehow requires offheap data to be in place.
 *
 */
public interface OffheapObject extends AutoCloseable {
    
    long memoryAddress();
}
