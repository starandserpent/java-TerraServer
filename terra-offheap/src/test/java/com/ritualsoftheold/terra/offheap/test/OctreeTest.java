package com.ritualsoftheold.terra.offheap.test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

import org.junit.Test;

import com.ritualsoftheold.terra.node.Octree;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.node.OffheapOctree;
import com.ritualsoftheold.terra.offheap.octree.OctreeStorage;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

import static org.junit.Assert.*;

public class OctreeTest {
    
    private static final Memory mem = OS.memory();
    
    @Test
    public void storageTest() {
        OctreeStorage storage = new OctreeStorage(2, null, ForkJoinPool.commonPool());
        long addr = mem.allocate(DataConstants.OCTREE_SIZE * 2);
        long addr2 = mem.allocate(DataConstants.OCTREE_SIZE * 2);
        storage.addOctrees((byte) 0, addr); // Add to storage
        storage.addOctrees((byte) 1, addr2);
        
        assertEquals(addr, storage.requestGroup((byte) 0).join().longValue());
        assertEquals(addr2, storage.requestGroup((byte) 1).join().longValue());
    }
    
    @Test
    public void octreeTest() {
        OffheapWorld world = new OffheapWorld(null, null, null);
        long addr = mem.allocate(DataConstants.OCTREE_SIZE * 8192); // Allocate memory
        mem.writeInt(addr + 1 + 6 * DataConstants.OCTREE_NODE_SIZE, 5); // Write one sample there
        
        world.getOctreeStorage().addOctrees((byte) 0, addr); // Add octrees (aka lots and lots of zeroes)!
        CompletableFuture<Octree> future = world.requestOctree(0); // Request first octree
        assertEquals(5, future.join().l_getNodeAt(6));
    }
    
}
