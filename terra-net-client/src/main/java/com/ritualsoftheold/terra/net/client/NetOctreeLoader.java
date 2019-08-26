package com.ritualsoftheold.terra.net.client;

import com.ritualsoftheold.terra.manager.DataConstants;
import com.ritualsoftheold.terra.manager.io.OctreeLoader;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

public class NetOctreeLoader implements OctreeLoader {

    private static final Memory mem = OS.memory();
    
    private int blockSize;
    
    public NetOctreeLoader(int blockSize) {
        this.blockSize = blockSize;
    }
    
    @Override
    public long loadOctrees(int newGroup, long addr) {
        addr = mem.allocate(DataConstants.OCTREE_GROUP_META + blockSize * DataConstants.OCTREE_SIZE);
        return addr;
    }

    @Override
    public void saveOctrees(int index, long addr) {
        // Saving data client-side makes no sense
    }

}
