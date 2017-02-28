package com.ritualsoftheold.terra.offheap.octree;

import com.ritualsoftheold.terra.offheap.DataConstants;

import it.unimi.dsi.fastutil.bytes.Byte2IntMap;
import it.unimi.dsi.fastutil.bytes.Byte2LongArrayMap;
import it.unimi.dsi.fastutil.bytes.Byte2LongMap;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Handles storage for all octrees of one world.
 *
 */
public class OctreeStorage {
    
    private static Memory mem = OS.memory();
    
    /**
     * Map of octree storage positions in memory.
     * Key is file index. Value is memory address.
     * All addresses have same amount of memory allocated after them.
     */
    private Byte2LongMap storageBlocks;
    
    /**
     * Size of storage blocks.
     */
    private int blockSize;
    
    public OctreeStorage(int blockSize) {
        this.blockSize = blockSize;
        this.storageBlocks = new Byte2LongArrayMap();
    }
    
    /**
     * Adds octrees with given index from given address. After this has been
     * done, do NOT touch the data following the memory address.
     * @param index Octree group index.
     * @param addr Memory address for data.
     */
    public void addOctrees(byte index, long addr) {
        storageBlocks.put(index, addr);
    }
    
    /**
     * Removes octrees of given index, then deallocates memory where they were.
     * @param index Octree group index.
     */
    public void removeOctrees(byte index) {
        mem.freeMemory(storageBlocks.remove(index), blockSize);
    }
    
    /**
     * Gets octree memory address for given octree index.
     * If that fails, -1 is returned.
     * @param index Octree index.
     * @return Octree address or -1.
     */
    public long getOctreeAddr(int index) {
        byte groupIndex = (byte) (index >>> 24);
        long blockAddr = storageBlocks.get(groupIndex);
        
        return blockAddr + DataConstants.OCTREE_SIZE * (index & 0xffffff);
    }
}
