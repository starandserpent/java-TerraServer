package com.ritualsoftheold.terra.offheap.node;

import java.util.Objects;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.node.Block;
import com.ritualsoftheold.terra.node.Chunk;
import com.ritualsoftheold.terra.node.SimpleBlock;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkUtils;
import com.ritualsoftheold.terra.offheap.data.OffheapNode;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

public class OffheapChunk implements Chunk, OffheapNode {

    private static Memory mem = OS.memory();
    
    /**
     * Memory address of block data.
     */
    private long address;
    
    /**
     * Is this chunk valid.
     */
    private boolean valid;
    
    private MaterialRegistry reg;

    @Override
    public Type getNodeType() {
        return Type.CHUNK;
    }

    @Override
    public long memoryAddress() {
        requireValid();
        
        return address;
    }

    @Override
    public void memoryAddress(long addr) {
        valid = true;
        
        address = addr;
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public void invalidate() {
        valid = false;
    }
    
    @Override
    public long l_getBlockMemoryAddr(float x, float y, float z) {
        requireValid();
        
        // Calculate offsets
        int x0 =  (int) (x * 0.25f);
        int y0 = (int) (y * 0.25f);
        int z0 = (int) (z * 0.25f);
        
        // Multiply offsets by coordinate multiplers (x=1, y=16, z=16²)
        return x0 * DataConstants.CHUNK_COORD_X + y0 * DataConstants.CHUNK_COORD_Y + z0 * DataConstants.CHUNK_COORD_Z;
    }
    
    @Override
    public short l_getMaterial(float x, float y, float z) {
        return mem.readShort(l_getBlockMemoryAddr(x, y, z));
    }

    @Override
    public Block getBlockAt(float x, float y, float z) {
        // TODO other block sizes
        return new SimpleBlock(reg.getForWorldId(l_getMaterial(x, y, z)), 0.25f);
    }
    
    @Override
    public void l_setMaterial(float x, float y, float z, short id, float scale) {
        // TODO actually use scale...
        mem.writeShort(l_getBlockMemoryAddr(x, y, z), id);
    }

    @Override
    public void setBlockAt(float x, float y, float z, Block block) {
        mem.writeShort(l_getBlockMemoryAddr(x, y, z), block.getMaterial().getWorldId());
    }

    @Override
    public int getMaxBlockCount() {
        return DataConstants.CHUNK_MAX_BLOCKS;
    }

    @Override
    public void getData(short[] data) {
        // OOOPS - might need to use raw Unsafe here...
    }

    @Override
    public void setData(short[] data) {
        // TODO Auto-generated method stub
    }

}
