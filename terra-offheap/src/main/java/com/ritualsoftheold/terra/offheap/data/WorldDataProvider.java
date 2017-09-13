package com.ritualsoftheold.terra.offheap.data;

/**
 * Provides access to some form of world data.
 *
 */
public interface WorldDataProvider {
    
    int newId();
    
    void write(int id, int offset, short[] data);
    
    void read(int id, int offset, short[] data);
    
    boolean isOctree();
}
