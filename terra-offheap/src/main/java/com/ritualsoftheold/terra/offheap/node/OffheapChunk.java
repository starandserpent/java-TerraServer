package com.ritualsoftheold.terra.offheap.node;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.node.Block;
import com.ritualsoftheold.terra.node.Chunk;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.data.MemoryRegion;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

public class OffheapChunk implements Chunk, OffheapNode {

    private static Memory mem = OS.memory();
    
    private long address;
    private int length;
    
    private MemoryRegion region;
    
    private MaterialRegistry reg;
    
    public OffheapChunk(MaterialRegistry reg, long address, int length) {
        this.reg = reg;
        this.address = address;
    }
    
    @Override
    public Block getBlockAt(float x, float y, float z) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setBlockAt(float x, float y, float z, Block block) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getMaxBlockCount() {
        return (int) (DataConstants.CHUNK_SCALE * DataConstants.CHUNK_SCALE * DataConstants.CHUNK_SCALE / DataConstants.SMALLEST_BLOCK);
    }

    @Override
    public void getData(short[] data) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setData(short[] data) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Type getNodeType() {
        return Type.CHUNK;
    }

    @Override
    public int l_getDataSize() {
        return length;
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
