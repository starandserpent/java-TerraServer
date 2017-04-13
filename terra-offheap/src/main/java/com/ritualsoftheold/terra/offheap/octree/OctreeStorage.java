package com.ritualsoftheold.terra.offheap.octree;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
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
    
    private OctreeLoader loader;
    
    private Executor loaderExecutor;
    
    public OctreeStorage(int blockSize, OctreeLoader loader, Executor executor) {
        this.loader = loader;
        this.loaderExecutor = executor;
        this.blockSize = blockSize;
        this.groups = new Byte2LongArrayMap();
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
    
    public CompletableFuture<Long> requestOctreeGroup(byte groupIndex) {
        long addr = groups.get(groupIndex >>> 24);
        if (addr == -1) {
            CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                long newAddr = mem.allocate(blockSize);
                loader.loadOctrees(groupIndex, newAddr);
                return newAddr;
            });
            return future;
        } else {
            return CompletableFuture.completedFuture(groups.get(groupIndex));
        }
    }
    
    /**
     * Gets octree memory address for given octree index using consumer.
     * @param index Octree index.
     * @param callback
     */
    public void getOctreeAddr(int index, LongConsumer callback) {
        byte groupIndex = (byte) (index >>> 24);
        // TODO do this with CompletableFuture
    }
}
