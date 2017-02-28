package com.ritualsoftheold.terra.offheap.io;

/**
 * Handles loading octree data blocks and possibly saving them.
 *
 */
public interface OctreeLoader {
    
    /**
     * Loads octrees based on group/file index.
     * @param index
     * @return Memory address where they were loaded.
     */
    long loadOctrees(byte index);
    
    /**
     * Saves octrees based on group/file index by reading data at given address.
     * @param index
     * @param addr
     */
    void saveOctrees(byte index, long addr);
}
