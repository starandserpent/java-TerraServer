package com.ritualsoftheold.terra.offheap.io.dummy;

import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.io.OctreeLoaderInterface;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

public class DummyOctreeLoader implements OctreeLoaderInterface {
    
    private static final Memory mem = OS.memory();
    
    private int blockSize;
    
    public DummyOctreeLoader(int blockSize) {
        this.blockSize = blockSize;
    }

    @Override
    public long loadOctrees(int index, long addr) {
        addr = mem.allocate(DataConstants.OCTREE_GROUP_META + blockSize * DataConstants.OCTREE_SIZE);
        mem.setMemory(addr, DataConstants.OCTREE_GROUP_META + blockSize * DataConstants.OCTREE_SIZE, (byte) 0);
        return addr;
    }

    @Override
    public void saveOctrees(int index, long addr) {
        // Do nothing
    }

}
