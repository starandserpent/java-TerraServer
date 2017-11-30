package com.ritualsoftheold.terra.offheap.io;

import com.ritualsoftheold.terra.offheap.Pointer;

/**
 * Handles loading octree data blocks and possibly saving them.
 *
 */
public interface OctreeLoader {
    
    /**
     * Loads octrees based on group/file index.
     * @param newGroup
     * @param addr Address where the caller would like data to be. If it
     * doesn't matter, it can be set to 0. This is only a hint, which some
     * implementations may choose to ignore.
     * @return Address where data was put.
     */
    long loadOctrees(int newGroup, @Pointer long addr);
    
    /**
     * Saves octrees based on group/file index by reading data at given address.
     * @param index
     * @param addr
     */
    void saveOctrees(int index, @Pointer long addr);
}
