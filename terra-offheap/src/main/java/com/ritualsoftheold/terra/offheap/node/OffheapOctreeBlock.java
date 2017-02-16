package com.ritualsoftheold.terra.offheap.node;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.material.TerraMaterial;
import com.ritualsoftheold.terra.node.Block;
import com.ritualsoftheold.terra.offheap.DataConstants;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

public class OffheapOctreeBlock implements Block, OffheapNode {
    
    private static Memory mem = OS.memory();
    
    private long address;
    private MaterialRegistry reg;
    
    public OffheapOctreeBlock(MaterialRegistry reg, long addr) {
        this.reg = reg;
        this.address = addr;
    }
    
    @Override
    public Type getNodeType() {
        return Type.BLOCK;
    }

    @Override
    public void setMaterial(TerraMaterial mat) {
        int data = mat.getWorldId() << 16; // 16 bits back; id comes first, then data
        mem.writeInt(address, data);
    }

    @Override
    public TerraMaterial getMaterial() {
        return reg.getForWorldId(mem.readShort(address));
    }

    @Override
    public int l_getDataSize() {
        return DataConstants.OCTREE_NODE_SIZE;
    }

    @Override
    public long memoryAddress() {
        return address;
    }

    @Override
    public void memoryAddress(long addr) {
        address = addr;
    }

}
