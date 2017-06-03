package com.ritualsoftheold.terra.offheap.octree;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.io.OctreeLoader;
import com.ritualsoftheold.terra.offheap.node.OffheapOctree;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;

import it.unimi.dsi.fastutil.bytes.Byte2LongArrayMap;
import it.unimi.dsi.fastutil.bytes.Byte2LongMap;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Handles storage for all octrees of one world.
 *
 */
public class OctreeStorage {
    
    private static final Memory mem = OS.memory();
    
    /**
     * Map of octree storage positions in memory.
     * Key is file index. Value is memory address.
     * All addresses have same amount of memory allocated after them.
     */
    private Byte2LongMap groups;
    
    /**
     * When octree groups were last needed.
     */
    private Byte2LongMap lastNeeded;
    
    /**
     * Group where new octrees should be added.
     */
    private byte freeGroup;
    
    /**
     * Free index in the {@link #freeGroup}. You should insert new octree at
     * this position, then increment this by one.
     */
    private int freeIndex;
    
    /**
     * Size of storage groups.
     */
    private int blockSize;
    
    private OctreeLoader loader;
    
    private Executor loaderExecutor;

    private int lastCreated;
    
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
    
    public CompletableFuture<Long> requestGroup(byte groupIndex) {
        long addr = groups.get(groupIndex);
        if (addr == -1) {
            CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                long newAddr = loader.loadOctrees(groupIndex, -1);
                groups.put(groupIndex, newAddr);
                return newAddr;
            }, loaderExecutor);
            return future;
        } else {
            return CompletableFuture.completedFuture(groups.get(groupIndex));
        }
    }
    
    public long getGroup(byte groupIndex) {
        long addr = groups.get(groupIndex);
        if (addr == -1) {
            loader.loadOctrees(groupIndex, -1);
            groups.put(groupIndex, addr);
        }
        
        // Mark that we needed the group
        lastNeeded.put(groupIndex, System.currentTimeMillis());
        
        return addr;
    }
    
    public CompletableFuture<Long> saveGroup(byte groupIndex) {
        long addr = groups.get(groupIndex);
        if (addr == -1) {
            throw new IllegalStateException("cannot save not loaded group");
        } else {
            CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                loader.saveOctrees(groupIndex, addr);
                return addr;
            });
            return future;
        }
    }
    
    public int newOctree() {
        int index = freeGroup << 24 | freeIndex; // Get next free index (includes group and octree indexes)
        
        // We just used the group, so mark it in map
        lastNeeded.put(freeGroup, System.currentTimeMillis());
        
        freeIndex++;
        if (freeIndex * DataConstants.OCTREE_SIZE == blockSize) { // This group just became full
            freeGroup++; // Take next group
            freeIndex = 0; // ... and zero index
        }
        
        lastCreated = index; // Store this as last created octree
        return index;
    }
    
    public int getLastOctree() {
        return lastCreated;
    }
    
    public int splitOctree(int index, int node) {
        byte groupIndex = (byte) (index >>> 24);
        int octreeIndex = index & 0xffffff;
        
        // Grab all necessary addresses
        long groupAddr = getGroup(groupIndex);
        long addr = groupAddr + octreeIndex * DataConstants.OCTREE_SIZE;
        long nodeAddr = addr + 1 + node * DataConstants.OCTREE_NODE_SIZE;
        
        int blockData = mem.readInt(nodeAddr); // Copy old block data
        
        // Gather new indexes and addresses
        int newIndex = newOctree(); // Get an index for new octree
        long newGroupAddr = getGroup((byte) (newIndex >>> 24)); // Get address for the new group
        long newAddr = newGroupAddr + (newIndex & 0xffffff) * DataConstants.OCTREE_NODE_SIZE;
        
        mem.writeByte(newAddr, (byte) 0); // New octree has single nodes only
        for (int i = 0; i < 8; i++) { // Copy old data to EVERY new node
            mem.writeInt(newAddr + i * DataConstants.OCTREE_NODE_SIZE, blockData);
        }
        
        // Now update old octree to properly point into it's no longer single child node
        mem.writeByte(addr, (byte) (mem.readByte(addr) | (1 << node))); // Update flags
        mem.writeInt(nodeAddr, newIndex); // ... and actual index
        
        return newIndex; // Finally return new index, as this is ready to be used
    }
    
    public OffheapOctree getOctree(int index, OffheapWorld world) {
        byte groupIndex = (byte) (index >>> 24);
        int octreeIndex = index & 0xffffff;
        long groupAddr = getGroup(groupIndex);
        // This future will block on groupFuture.get()... hopefully
        long addr = groupAddr + octreeIndex * DataConstants.OCTREE_SIZE;
        
        OffheapOctree octree = new OffheapOctree(world, 0f); // TODO scale, somehow
        octree.memoryAddress(addr); // Validate octree with memory address!
        
        return octree;
    }
    
    public long getLastNeeded(byte groupId) {
        return lastNeeded.get(groupId);
    }
}
