package com.ritualsoftheold.terra.offheap.octree;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

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
     * Memory address of data (kinda array) which stores memory addresses.
     */
    private long groups;
    
    /**
     * Memory address of data which contains timestamps for when
     * octree data was last needed.
     */
    private long lastNeeded;
    
    /**
     * Group where new octrees should be added.
     */
    private AtomicInteger freeGroup;
    
    /**
     * Free index in the {@link #freeGroup}. You should insert new octree at
     * this position, then increment this by one.
     */
    private AtomicInteger freeIndex;
    
    /**
     * Size of storage groups.
     */
    private int blockSize;
    
    private OctreeLoader loader;
    
    private Executor loaderExecutor;
    
    public OctreeStorage(int blockSize, OctreeLoader loader, Executor executor) {
        this.loader = loader;
        this.loaderExecutor = executor;
        this.freeGroup = new AtomicInteger();
        this.freeIndex = new AtomicInteger();
        this.blockSize = blockSize;
        this.groups = mem.allocate(256 * 8); // 256 longs at most
        this.lastNeeded = mem.allocate(256 * 8);
    }
    
    private long getGroupsAddr(byte index) {
        return groups + index * 8;
    }
    
    private long getTimestampAddr(byte index) {
        return lastNeeded + index * 8;
    }
    
    /**
     * Adds octrees with given index from given address. After this has been
     * done, do NOT touch the data following the memory address.
     * @param index Octree group index.
     * @param addr Memory address for data.
     */
    public void addOctrees(byte index, long addr) {
        mem.writeVolatileLong(getGroupsAddr(index), addr);
    }
    
    /**
     * Removes octrees of given index, then deallocates memory where they were.
     * @param index Octree group index.
     */
    public void removeOctrees(byte index) {
        mem.freeMemory(mem.readVolatileLong(getGroupsAddr(index)), blockSize);
    }
    
    public long getGroup(byte groupIndex) {
        long addr = mem.readVolatileLong(getGroupsAddr(groupIndex));
        if (addr == -1) {
            loader.loadOctrees(groupIndex, -1);
            mem.writeVolatileLong(getGroupsAddr(groupIndex), addr);
        }
        
        // Mark that we needed the group
        mem.writeVolatileLong(getTimestampAddr(groupIndex), System.currentTimeMillis());
        
        return addr;
    }
    
    public CompletableFuture<Long> saveGroup(byte groupIndex) {
        long addr = mem.readVolatileLong(getGroupsAddr(groupIndex));
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
        int octreeIndex = freeIndex.getAndIncrement(); // Get octree index (not group) and increment it by one
        // We just used the group, so mark it in map
        //lastNeeded.put(freeGroup, System.currentTimeMillis());
        
        int groupIndex = freeGroup.get(); // TODO I hope this is safe
        if (freeIndex.get() * DataConstants.OCTREE_SIZE == blockSize) { // This group just became full
            freeGroup.compareAndSet(groupIndex, groupIndex + 1); // Take next group if someone didn't do yet do it
            freeIndex.compareAndSet(blockSize, 0); // ... and zero index if someone didn't yet do it
        }
        
        return groupIndex << 24 | octreeIndex;
    }
    
    public int getNextIndex() {
        return freeGroup.get() << 24 | freeIndex.get(); // Get next free index (includes group and octree indexes)
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
        return mem.readVolatileLong(getTimestampAddr(groupId));
    }
}
