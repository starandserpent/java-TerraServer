package com.ritualsoftheold.terra.offheap.node;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.material.TerraMaterial;
import com.ritualsoftheold.terra.node.Chunk;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.data.OffheapNode;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

public class OffheapChunk implements Chunk, OffheapNode {

    private static final Memory mem = OS.memory();
    
    /**
     * Buffer which holds this chunk.
     */
    private ChunkBuffer buf;
    
    /**
     * Index of this chunk IN the buffer.
     */
    private int chunkId;
    
    private MaterialRegistry materialRegistry;
    
    public OffheapChunk(ChunkBuffer buf, int chunkId, MaterialRegistry materialRegistry) {
        this.buf = buf;
        this.chunkId = chunkId;
        this.materialRegistry = materialRegistry;
    }

    @Override
    public Type getNodeType() {
        return Type.CHUNK;
    }

    @Override
    public long memoryAddress() {
        return buf.getChunkAddr(chunkId);
    }

    @Override
    public int getMaxBlockCount() {
        return DataConstants.CHUNK_MAX_BLOCKS;
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
    public short getBlockId(int index) {
        return buf.getBlock(index, chunkId);
    }

    @Override
    public void setBlockId(int index, short id) {
        buf.queueChange(chunkId, index, id);
    }

    @Override
    public void getBlockIds(int[] indices, short[] ids) {
        buf.getBlocks(chunkId, indices, ids);
    }

    @Override
    public void setBlockIds(int[] indices, short[] ids) {
        // TODO multi-change queries
        for (int i = 0; i < indices.length; i++) {
            buf.queueChange(chunkId, indices[i], ids[i]);
        }
    }

    @Override
    public TerraMaterial getBlock(int index) {
        return materialRegistry.getForWorldId(getBlockId(index));
    }

    @Override
    public void setBlock(int index, TerraMaterial material) {
        setBlockId(index, material.getWorldId());
    }

    @Override
    public void close() throws Exception {
        // Do nothing. No need to close this
    }

}
