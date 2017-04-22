package com.ritualsoftheold.terra.offheap.test;

import java.util.concurrent.ForkJoinPool;

import org.junit.Test;

import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.octree.OctreeStorage;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

import static org.junit.Assert.*;

public class OctreeTest {
    
    private static final Memory mem = OS.memory();
    
    @Test
    public void storageTest() {
        OctreeStorage storage = new OctreeStorage(1, null, ForkJoinPool.commonPool());
        long addr = mem.allocate(DataConstants.OCTREE_SIZE);
        storage.addOctrees((byte) 0, addr); // Add to storage
        
        storage.requestGroup((byte) 0).thenAccept((newAddr) -> {
            assertEquals(addr, newAddr.longValue());
        });
    }
}
