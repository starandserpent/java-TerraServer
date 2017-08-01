package com.ritualsoftheold.terra.offheap.test;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

import org.junit.Test;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.node.Octree;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyChunkLoader;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyOctreeLoader;
import com.ritualsoftheold.terra.offheap.memory.MemoryPanicHandler;
import com.ritualsoftheold.terra.offheap.octree.OctreeStorage;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;
import com.ritualsoftheold.terra.world.gen.EmptyWorldGenerator;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

public class OctreeTest {
    
    private static final Memory mem = OS.memory();
    
    @Test
    public void storageTest() {
        OctreeStorage storage = new OctreeStorage(2, new DummyOctreeLoader(2), ForkJoinPool.commonPool());
        storage.setMemListener(new DummyMemoryUseListener());
        long addr = mem.allocate(DataConstants.OCTREE_GROUP_META + DataConstants.OCTREE_SIZE * 2);
        long addr2 = mem.allocate(DataConstants.OCTREE_GROUP_META + DataConstants.OCTREE_SIZE * 2);
        storage.addOctrees((byte) 0, addr); // Add to storage
        storage.addOctrees((byte) 1, addr2);
        
        assertEquals(addr + DataConstants.OCTREE_GROUP_META, storage.getGroup((byte) 0));
        assertEquals(addr2 + DataConstants.OCTREE_GROUP_META, storage.getGroup((byte) 1));
    }
    
    @Test
    public void octreeTest() {
        OffheapWorld world = new OffheapWorld(new DummyChunkLoader(), new DummyOctreeLoader(8192), new MaterialRegistry(), new EmptyWorldGenerator());
        
        world.setMemorySettings(0, 0, new MemoryPanicHandler() {
            
            @Override
            public PanicResult outOfMemory(long max, long used, long possible) {
                return PanicResult.CONTINUE;
            }
            
            @Override
            public boolean handleFreeze(long stamp) {
                return false;
            }
            
            @Override
            public PanicResult goalNotMet(long goal, long possible) {
                return PanicResult.CONTINUE;
            }
        });
        long addr = mem.allocate(DataConstants.OCTREE_SIZE * 8192); // Allocate memory
        mem.writeInt(addr + DataConstants.OCTREE_GROUP_META + 1 + 6 * DataConstants.OCTREE_NODE_SIZE, 5); // Write one sample there
        
        world.getOctreeStorage().addOctrees((byte) 0, addr); // Add octrees (aka lots and lots of zeroes)!
        assertEquals(5, world.getOctreeStorage().getOctree(0, world).l_getNodeAt(6));
    }
    
}
