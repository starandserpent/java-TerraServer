package com.ritualsoftheold.terra.offheap.io.dummy;

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
        return mem.allocate(blockSize);
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
