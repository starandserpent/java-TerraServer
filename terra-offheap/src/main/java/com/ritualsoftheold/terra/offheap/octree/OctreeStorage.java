package com.ritualsoftheold.terra.offheap.octree;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.io.OctreeLoader;
import com.ritualsoftheold.terra.offheap.memory.MemoryUseListener;
import com.ritualsoftheold.terra.offheap.node.OffheapOctree;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Handles storage for all octrees of one world.
 *
 */
public class OctreeStorage {
    
    private static final Memory mem = OS.memory();
    
    /**
     * Memory address of data (kinda array) which stores memory addresses.
     */
    private AtomicLongArray groups;
    
    /**
     * Memory address of data which contains timestamps for when
     * octree data was last needed.
     */
    private AtomicLongArray lastNeeded;
    
    /**
     * How many octrees goes to one storage group.
     */
    private int blockSize;
    
    /**
     * Octree loader implementation.
     */
    private OctreeLoader loader;
    
    /**
     * Executor which will be used for asynchronous tasks.
     */
    private Executor loaderExecutor;
    
    /**
     * Memory listener which is to be notified when memory is
     * allocated or freed.
     */
    private MemoryUseListener memListener;
    
    private long countAddr;
    
    public OctreeStorage(int blockSize, OctreeLoader loader, Executor executor, MemoryUseListener memListener) {
        this.loader = loader;
        this.loaderExecutor = executor;
        this.memListener = memListener;
        this.blockSize = blockSize;
        this.groups = new AtomicLongArray(256); // 256 groups at most
        this.lastNeeded = new AtomicLongArray(256);
        
        // Load master group and cache count address
        countAddr = getGroupMeta(0);
        if (mem.readVolatileInt(countAddr) == 0) { // Point over master group, we don't add octrees there normally
            mem.writeVolatileInt(countAddr, blockSize - 1);
        }
    }
    
    /**
     * Adds octrees with given index from given address. After this has been
     * done, do NOT touch the data following the memory address.
     * @param index Octree group index.
     * @param addr Memory address for data.
     * @return If it succeeded or failed.
     */
    public boolean addOctrees(int index, long addr) {
        return groups.compareAndSet(index, 0, addr);
    }
    
    /**
     * Removes octrees of given index, then deallocates memory where they were.
     * @param index Octree group index.
     * @param saveFirst If group should be saved before it is removed from memory.
     * @return If it succeeded or not.
     */
    public boolean removeOctrees(int index, boolean saveFirst) {
        long addr = groups.getAndSet(index, 0);
        if (addr != 0) {
            int amount = blockSize + DataConstants.OCTREE_GROUP_META;
            if (saveFirst) {
                saveGroup(index).thenRun(() -> {
                    mem.freeMemory(addr, amount);
                    memListener.onFree(amount);
                });
                return true;
            } else {
                mem.freeMemory(addr, amount);
                memListener.onFree(amount);
            }
            return true;
        }
        return false;
    }
    
    public long getGroup(int group) {
        long addr = groups.get(group);
        if (addr == 0) {
            addr = loader.loadOctrees(group, 0);
            if (groups.compareAndSet(group, 0, addr)) {
                memListener.onAllocate(blockSize);
            } else { // Another thread was quicker, we should just free memory now
                mem.freeMemory(addr, blockSize + DataConstants.OCTREE_GROUP_META);
            }
        }
        
        // Mark that we needed the group
        lastNeeded.set(group, System.currentTimeMillis());
        
        return addr + DataConstants.OCTREE_GROUP_META;
    }
    
    public long getGroupMeta(int groupIndex) {
        return getGroup(groupIndex) - DataConstants.OCTREE_GROUP_META;
    }
    
    public CompletableFuture<Long> saveGroup(int groupIndex) {
        long addr = groups.get(groupIndex);
        if (addr == 0) {
            // The group is not there, perhaps it was made inactive
            return null;
        } else {
            CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                loader.saveOctrees(groupIndex, addr);
                return addr;
            }, loaderExecutor);
            return future;
        }
    }
    
    /**
     * Creates a new octree and provides you with full id of it.
     * @return Octree id.
     */
    public int newOctree() {
        int freeIndex = mem.addInt(countAddr, 1); // addInt functions like incrementAndGet in this case
        int group = freeIndex / blockSize; // Find octree group (unsigned byte, < 256)
        int index = freeIndex % blockSize; // Find index inside that group
        int id = group << 24 | index;
        
        // Make octrees contents "null" by making flags 1
        mem.writeVolatileByte(getOctreeAddr(id), (byte) 1);
        
        // Stitch group and index together to get full id!
        return id;
    }
    
    /**
     * Gets amount of groups currently in use.
     * @return Group count.
     */
    public int getGroupCount() {
        return mem.readVolatileInt(countAddr) / blockSize + 1;
    }
    
    // I guess it works, unit tested and all...
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
        
        mem.writeVolatileByte(newAddr, (byte) 0); // New octree has single nodes only
        for (int i = 0; i < 8; i++) { // Copy old data to EVERY new node
            mem.writeVolatileInt(newAddr + i * DataConstants.OCTREE_NODE_SIZE, blockData);
        }
        
        // Now update old octree to properly point into it's no longer single child node
        mem.writeVolatileByte(addr, (byte) (mem.readVolatileByte(addr) | (1 << node))); // Update flags
        mem.writeVolatileInt(nodeAddr, newIndex); // ... and actual index
        
        return newIndex; // Finally return new index, as this is ready to be used
    }
    
    public OffheapOctree getOctree(int index, MaterialRegistry registry) {
        long addr = getOctreeAddr(index);
        
        // TODO memory management aka UserOffheapOctree
        return new OffheapOctree(addr, index, registry);
    }
    
    public long getOctreeAddr(int index) {
        int groupIndex = index >>> 24;
        int octreeIndex = index & 0xffffff;
        long groupAddr = getGroup(groupIndex);
        return groupAddr + octreeIndex * DataConstants.OCTREE_SIZE;
    }

    public AtomicLongArray getGroups() {
        return groups;
    }
    
    public int getGroupSize() {
        return blockSize * DataConstants.OCTREE_SIZE + DataConstants.OCTREE_GROUP_META;
    }

    /**
     * Retrieves master octree full index. Usually it has group 0.
     * @return Octree index for master octree.
     */
    public int getMasterIndex() {
        return mem.readInt(getGroupMeta((byte) 0) + 4);
    }
    
    /**
     * Gets current scale of master octree.
     * @param def If scale is not available, will be set to this.
     * @return Current scale.
     */
    public float getMasterScale(float def) {
        long addr = getGroupMeta((byte) 0) + 8;
        float val = mem.readFloat(addr);
        if (val == 0) {
            mem.writeFloat(addr, def);
            return def;
        }
        return val;
    }

    public long getMasterGroupAddr() {
        return groups.get(0);
    }

    public float getCenterPoint(int type) {
        long addr = getGroupMeta((byte) 0) + 12 + type * 4;
        return mem.readFloat(addr);
    }
}
