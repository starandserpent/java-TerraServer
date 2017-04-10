package com.ritualsoftheold.terra.offheap.octree;

import java.util.concurrent.BlockingQueue;
import java.util.function.LongConsumer;

import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.io.OctreeLoader;
import com.ritualsoftheold.terra.offheap.octree.OctreeLoaderThread.GroupEntry;

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
    private Byte2LongMap groups;
    
    /**
     * Size of storage groups.
     */
    private int blockSize;
    
    private BlockingQueue<GroupEntry> loaderQueue;
    
    private OctreeLoaderThread[] loaderThreads;
    
    public OctreeStorage(int blockSize, OctreeLoader loader, int threadCount) {
        initLoaderThreads(loader, threadCount);
        this.blockSize = blockSize;
        this.groups = new Byte2LongArrayMap();
    }
    
    private void initLoaderThreads(OctreeLoader loader, int threadCount) {
        loaderThreads = new OctreeLoaderThread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            loaderThreads[i] = new OctreeLoaderThread(loaderQueue, loader);
        }
    }
    
    /**
     * Adds octrees with given index from given address. After this has been
     * done, do NOT touch the data following the memory address.
     * @param index Octree group index.
     * @param addr Memory address for data.
     */
    public void addOctrees(byte index, long addr) {
        groups.put(index, addr);
    }
    
    /**
     * Removes octrees of given index, then deallocates memory where they were.
     * @param index Octree group index.
     */
    public void removeOctrees(byte index) {
        mem.freeMemory(groups.remove(index), blockSize);
    }
    
    public void requestOctreeGroup(byte groupIndex, LongConsumer callback) {
        long addr = groups.get(groupIndex >>> 24);
        if (addr == -1) {
            GroupEntry entry = new GroupEntry(false, groupIndex, mem.allocate(blockSize), (groupAddr -> {
                groups.put(groupIndex, groupAddr);
                callback.accept(groupAddr);
            }) );
            loaderQueue.add(entry);
        } else {
            callback.accept(addr);
        }
    }
    
    /**
     * Gets octree memory address for given octree index using consumer.
     * @param index Octree index.
     * @param callback
     */
    public void getOctreeAddr(int index, LongConsumer callback) {
        byte groupIndex = (byte) (index >>> 24);
        requestOctreeGroup(groupIndex, (startAddr -> {
            callback.accept(startAddr + DataConstants.OCTREE_SIZE * (index & 0xffffff));
        }));
    }
}
