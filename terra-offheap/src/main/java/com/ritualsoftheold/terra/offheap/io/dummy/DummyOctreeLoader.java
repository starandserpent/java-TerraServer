package com.ritualsoftheold.terra.offheap.io.dummy;

import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.io.OctreeLoader;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

public class DummyOctreeLoader implements OctreeLoader {
    
    private static final Memory mem = OS.memory();
    
    private int blockSize;
    
    public DummyOctreeLoader(int blockSize) {
        this.blockSize = blockSize;
    }

    @Override
    public long loadOctrees(byte index, long addr) {
        addr = mem.allocate(blockSize * DataConstants.OCTREE_SIZE);
        mem.setMemory(addr, blockSize * DataConstants.OCTREE_SIZE, (byte) 0);
        return addr;
    }

    @Override
    public void saveOctrees(byte index, long addr) {
        // Do nothing
    }

    @Override
    public int countGroups() {
        return 0;
    }

}
