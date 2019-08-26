package com.ritualsoftheold.terra.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;

import com.ritualsoftheold.terra.manager.DataConstants;
import com.ritualsoftheold.terra.manager.io.dummy.DummyOctreeLoader;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

public class OctreeTest {
    
    private static final Memory mem = OS.memory();
    
    private OctreeStorage storage;
    
    @Before
    public void init() {
        storage = new OctreeStorage(32768, new DummyOctreeLoader(32768), Executors.newCachedThreadPool(), new DummyMemoryUseListener(), false, null);
    }
    
    @Test
    public void addGetRemove() {
        long addr = mem.allocate(DataConstants.OCTREE_SIZE * 32758 + DataConstants.OCTREE_GROUP_META);
        assertTrue(storage.addOctrees(1, addr));
        assertEquals(addr, storage.getGroupMeta(1));
        assertEquals(addr + DataConstants.OCTREE_GROUP_META, storage.getOctreeAddr(1 << 24));
        assertTrue(storage.removeOctrees(1, false));
    }
    
    @Test
    public void newOctree() {
        for (int i = 0; i < 32768; i++) {
            assertEquals(1 << 24 | i, storage.newOctree());
        }
        for (int i = 0; i < 32768; i++) {
            assertEquals(2 << 24 | i, storage.newOctree());
        }
        assertEquals(3, storage.getGroupCount());
    }
    
    @Test
    public void splitOctree() {
        long addr = mem.allocate(DataConstants.OCTREE_SIZE * 32758 * DataConstants.OCTREE_GROUP_META);
        storage.addOctrees(1, addr);
        int index = storage.newOctree();
        assertEquals(1 << 24 | 1, storage.splitOctree(index, 1));
    }
    
    @Test
    public void masterScale() {
        assertEquals(16, storage.getMasterScale(16), 0.0001);
        assertEquals(16, storage.getMasterScale(8), 0.0001); // Does default work as fallback only?
    }
}
