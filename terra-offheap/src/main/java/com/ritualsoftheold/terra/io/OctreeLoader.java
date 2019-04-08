package com.ritualsoftheold.terra.io;

import com.ritualsoftheold.terra.Pointer;

/**
 * Handles loading octree data blocks and possibly saving them.
 *
 */
public interface OctreeLoader {
    
    /**
     * Loads octrees based on group/file index.
     * @param newGroup
     * @param address Address where the caller would like data to be. If it
     * doesn't matter, it can be set to 0. This is only a hint, which some
     * implementations may choose to ignore.
     * @return Address where data was put.
     */
    long loadOctrees(int newGroup, @Pointer long addr);
    
    /**
     * Saves octrees based on group/file index by reading data at given address.
     * @param index
     * @param address
     */
    void saveOctrees(int index, @Pointer long addr);
}
