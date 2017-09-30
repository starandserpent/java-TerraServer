package com.ritualsoftheold.terra.offheap.node;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.node.Block;
import com.ritualsoftheold.terra.node.Chunk;
import com.ritualsoftheold.terra.node.SimpleBlock;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkStorage;
import com.ritualsoftheold.terra.offheap.data.OffheapNode;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

public class OffheapChunk implements Chunk, OffheapNode {

    private static final Memory mem = OS.memory();
    
    /**
     * Buffer which holds this chunk.
     */
    private ChunkBuffer buf;
    
    private int index;

    @Override
    public Type getNodeType() {
        return Type.CHUNK;
    }

    @Override
    public long memoryAddress() {
        return buf.getChunkAddr(index);
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
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void getData(short[] data) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setData(short[] data) {
        // TODO Auto-generated method stub
        
    }
    
    

}
