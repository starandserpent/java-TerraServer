package com.ritualsoftheold.terra.offheap.node;

import com.ritualsoftheold.terra.node.Block;
import com.ritualsoftheold.terra.node.Chunk;
import com.ritualsoftheold.terra.node.Node;
import com.ritualsoftheold.terra.node.Octree;
import com.ritualsoftheold.terra.offheap.DataConstants;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

public class OffheapOctree implements Octree {
    
    private static Memory mem = OS.memory();
    
    private long address;
    
    @Override
    public Type getNodeType() {
        return Type.OCTREE;
    }

    @Override
    public Node getNodeAt(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Octree getOctreeAt(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Block getBlockAt(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Chunk getChunkAt(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Node[] getNodes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long l_getAddress() {
        return address;
    }

    @Override
    public int l_getSize() {
        return DataConstants.OCTREE_SIZE;
    }

    @Override
    public long l_getNodeAddr(int index) {
        return address + DataConstants.OCTREE_NODE_SIZE * index;
    }

    @Override
    public int l_getNodeAt(int index) {
        return mem.readInt(l_getNodeAddr(index));
    }

    @Override
    public void l_getData(int[] data) {
        if (data.length < 9)
            throw new IllegalArgumentException("data array must be at least 9 ints");
        // First int, least significant: flags
        // Other ints: octree data
        mem.copyMemory(address, data, DataConstants.ARRAY_DATA + 2, DataConstants.OCTREE_SIZE);
    }

}
