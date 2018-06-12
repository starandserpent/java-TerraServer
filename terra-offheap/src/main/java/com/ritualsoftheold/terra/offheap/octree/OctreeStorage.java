package com.ritualsoftheold.terra.offheap.octree;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.Pointer;
import com.ritualsoftheold.terra.offheap.io.OctreeLoader;
import com.ritualsoftheold.terra.offheap.memory.MemoryUseListener;
import com.ritualsoftheold.terra.offheap.node.OffheapOctree;
import com.ritualsoftheold.terra.offheap.world.OffheapLoadMarker;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Handles storage for all octrees of one world.
 *
 */
public class OctreeStorage {
    
    private static final Memory mem = OS.memory();
    
    /**
     * Memory address of octree groups.
     */
    private final AtomicLongArray groups;
    
    /**
     * Availability data addresses for octree groups.
     */
    private final AtomicLongArray availability;
    
    /**
     * Memory address of data which contains timestamps for when
     * octree data was last needed.
     */
    private final AtomicLongArray lastNeeded;
    
    private final AtomicIntegerArray userCounts;
    
    /**
     * How many octrees goes to one storage group.
     */
    private final int blockSize;
    
    /**
     * Octree loader implementation.
     */
    private final OctreeLoader loader;
    
    /**
     * Executor which will be used for asynchronous tasks.
     */
    private final Executor loaderExecutor;
    
    /**
     * Memory listener which is to be notified when memory is
     * allocated or freed.
     */
    private final MemoryUseListener memListener;
    
    private final long countAddr;
    
    private final UsageListener usageListener;
        
    public OctreeStorage(int blockSize, OctreeLoader loader, Executor executor, MemoryUseListener memListener,
            boolean availibility, UsageListener usageListener) {
        this.loader = loader;
        this.loaderExecutor = executor;
        this.memListener = memListener;
        this.blockSize = blockSize;
        this.groups = new AtomicLongArray(256); // 256 groups at most
        this.lastNeeded = new AtomicLongArray(256);
        this.userCounts = new AtomicIntegerArray(256);
        
        // Enable availability checking if needed
        // Important: this must be done before group 0 is loaded
        // (otherwise, JVM segfaults due to NULL dereference)
        if (availibility) {
            this.availability = new AtomicLongArray(256);
        } else {
            this.availability = null;
        }
        
        this.usageListener = usageListener;
        
        // Load master group and cache count address
        countAddr = getGroupMeta(0);
        if (mem.readVolatileInt(countAddr) == 0) { // Point over master group, we don't add octrees there normally
            mem.writeVolatileInt(countAddr, blockSize - 1);
        }
    }
    
    /**
     * Gets amount of memory (in bytes) that given amount of octrees uses.
     * @param count How many octrees.
     * @return Size of said octrees.
     */
    private int getOctreesSize(int count) {
        return count * DataConstants.OCTREE_SIZE;
    }
    
    /**
     * Adds octrees with given index from given address. After this has been
     * done, do NOT touch the data following the memory address.
     * @param index Octree group index.
     * @param address Memory address for data.
     * @return If it succeeded or failed.
     */
    public boolean addOctrees(int index, @Pointer long addr) {
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
            // Once used mark gets to zero, we can unload
            while (true) { // Spin loop until used < 1
                Thread.onSpinWait();
                int used = getUsedCount(index);
                if (used < 1) {
                    break;
                }
            }
            
            int amount = getOctreesSize(blockSize) + DataConstants.OCTREE_GROUP_META;
            if (saveFirst) {
                saveGroup(index, addr).thenRun(() -> {
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
    
    /**
     * Gets an address for octree group. You must make sure to mark it used
     * before doing this and marking it unused after you are done. Failure to
     * do so probably causes segfaults or worse.
     * @param group Group index.
     * @return Address for group.
     */
    public long getGroup(int group) {
        long addr = groups.get(group); // This does OOB check
        if (addr == 0) {
            addr = loader.loadOctrees(group, 0); // Loader will assign the address for now
            if (groups.compareAndSet(group, 0, addr)) {
                memListener.onAllocate(getOctreesSize(blockSize) + DataConstants.OCTREE_GROUP_META);
                // Tell the memory listener that we'll keep this RAM
                
                // Availability storage
                if (availability != null) {
                    long avAddr = availability.get(group);
                    if (avAddr == 0) { // Need to allocate
                        avAddr = mem.allocate(blockSize); // 1 byte per octree
                    }
                    mem.setMemory(avAddr, blockSize, (byte) 0); // Clear whatever was there
                    availability.set(group, avAddr);
                }
            } else { // Another thread was quicker, we should just free memory now
                mem.freeMemory(addr, getOctreesSize(blockSize) + DataConstants.OCTREE_GROUP_META);
            }
        }
        
        // Mark that we needed the group
        lastNeeded.set(group, System.currentTimeMillis());
        
        return addr + DataConstants.OCTREE_GROUP_META;
    }
    
    public long getGroupMeta(int groupIndex) {
        return getGroup(groupIndex) - DataConstants.OCTREE_GROUP_META;
    }
    
    public CompletableFuture<Long> saveGroup(int groupIndex, @Pointer long addr) {
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
        markUsed(group); // Group must not be unloaded while we are working on it
        int index = freeIndex % blockSize; // Find index inside that group
        int id = group << 24 | index;
        
        // Make octrees contents "null" by making flags 1
        mem.writeVolatileByte(getOctreeAddr(id), (byte) 0xff);
        markUnused(group); // Now, it could be unloaded. At least it is safe
        
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
        markUsed(groupIndex);
        int octreeIndex = index & 0xffffff;
        
        // Grab all necessary addresses
        long groupAddr = getGroup(groupIndex);
        long addr = groupAddr + octreeIndex * DataConstants.OCTREE_SIZE;
        long nodeAddr = addr + 1 + node * DataConstants.OCTREE_NODE_SIZE;
        
        int blockData = mem.readInt(nodeAddr); // Copy old block data
        
        // Gather new indexes and addresses
        int newIndex = newOctree(); // Get an index for new octree
        int newGroup = newIndex >>> 24;
        markUsed(newGroup);
        
        long newGroupAddr = getGroup(newGroup); // Get address for the new group
        long newAddr = newGroupAddr + (newIndex & 0xffffff) * DataConstants.OCTREE_NODE_SIZE;
        
        mem.writeVolatileByte(newAddr, (byte) 0); // New octree has single nodes only
        for (int i = 0; i < 8; i++) { // Copy old data to EVERY new node
            mem.writeVolatileInt(newAddr + i * DataConstants.OCTREE_NODE_SIZE, blockData);
        }
        
        // Now update old octree to properly point into it's no longer single child node
        mem.writeVolatileByte(addr, (byte) (mem.readVolatileByte(addr) | (1 << node))); // Update flags
        mem.writeVolatileInt(nodeAddr, newIndex); // ... and actual index
        
        // Mark both new and old buffers unused
        markUnused(groupIndex);
        markUnused(newGroup);
        
        return newIndex; // Finally return new index, as this is ready to be used
    }
    
    public OffheapOctree getOctree(int index, MaterialRegistry registry) {
        long addr = getOctreeAddr(index);
        
        // TODO memory management aka UserOffheapOctree
        return new OffheapOctree(addr, index, registry);
    }
    
    public long getOctreeAddrInternal(int index) {
        int groupIndex = index >>> 24; // Unsigned shift!
        int octreeIndex = index & 0xffffff; // Unsigned, but fits in to int: still 7 bits unused
        long groupAddr = getGroup(groupIndex);
        
        return groupAddr + octreeIndex * DataConstants.OCTREE_SIZE;
    }
    
    public long getOctreeAddr(int index) {
        int groupIndex = index >>> 24; // Unsigned shift!
        int octreeIndex = index & 0xffffff; // Unsigned, but fits in to int: still 7 bits unused
        long groupAddr = getGroup(groupIndex);
        
        // Ensure that octree is available
        if (availability != null) { // Optimized: either triggered always (client) or never (server)
            while (true) {
                long avData = availability.get(groupIndex);
                if (avData == 0) {
                    // Group is not yet ready. Spin-wait
                    Thread.onSpinWait();
                    continue;
                }
                while (mem.readVolatileByte(avData + octreeIndex) == 0) {
                    // Group is ready, but is the individual octree also ready?
                    Thread.onSpinWait();
                }
            }
        }
        return groupAddr + octreeIndex * DataConstants.OCTREE_SIZE;
    }
    
    public void setAvailability(int id, byte available) {
        int groupIndex = id >>> 24;
        int octreeIndex = id & 0xffffff;
        
        long avData = availability.get(groupIndex);
        mem.writeVolatileByte(avData + octreeIndex, available);
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
        return mem.readVolatileInt(getGroupMeta((byte) 0) + 4);
    }
    
    /**
     * Gets current scale of master octree.
     * @param def If scale is not available, will be set to this.
     * @return Current scale.
     */
    public float getMasterScale(float def) {
        long addr = getGroupMeta((byte) 0) + 8;
        float val = mem.readVolatileFloat(addr);
        if (val == 0) {
            mem.writeVolatileFloat(addr, def);
            return def;
        }
        return val;
    }

    public long getMasterGroupAddr() {
        return groups.get(0);
    }

    public float getCenterPoint(int type) {
        long addr = getGroupMeta(0) + 12 + type * 4;
        return mem.readVolatileFloat(addr);
    }
    
    public int markUsed(int index) {
        return userCounts.incrementAndGet(index);
    }
    
    public int markUnused(int index) {
        return userCounts.decrementAndGet(index);
    }
    
    public int getUsedCount(int index) {
        return userCounts.get(index);
    }
    
    public void removeLoadMarker(OffheapLoadMarker marker) {
        int[] octrees = marker.getOctrees();
        for (int i = 0; i < octrees.length; i++) {
            int id = octrees[i];
            markUnused(id >>> 24);
            if (usageListener != null) {
                usageListener.unused(id);
            }
        }
    }
    
}
