package com.ritualsoftheold.terra.offheap.node;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.node.Block;
import com.ritualsoftheold.terra.node.Chunk;
import com.ritualsoftheold.terra.offheap.ChunkUtils;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.data.MemoryRegion;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

// TODO why did I think that storing whole CHUNK offheap would be good idea?
public class OffheapChunk implements Chunk, OffheapNode {

    private static Memory mem = OS.memory();
    
    private long address;
    private int length;
    private int reserved;
    
    /**
     * Determines if this chunk uses material atlas.
     * For now, always true: need to concentrate on one thing at time.
     */
    private boolean hasAtlas = true;
    
    private MemoryRegion region;
    
    private MaterialRegistry reg;
    
    // Cache these, its faster
    private long sizesAddr = sizesAddr();
    private long blocksAddr = blocksAddr();
    private int bytesPerBlock = hasAtlas ? 1 : 2;
    
    public OffheapChunk(MaterialRegistry reg, long address, int length, int reserved) {
        this.reg = reg;
        this.address = address;
        this.reserved = reserved;
    }
    
    @Override
    public Block getBlockAt(float x, float y, float z) {
        float dist = ChunkUtils.distance(x, y, z);
        float traveled = 0f;
        int blockCount = 0;
        
        short blockId;
        float blockScale;
        
        /*
         * We will try to figure out correct address for the "distance".
         * Since there can be 32 flags in one long, this is quite fast - I hope.
         */
        outer: while (true) {
            long scales = mem.readLong(sizesAddr());
            
            for (int i = 32; i > 0; i--) {
                long scaleFlag = scales >>> (i * 2) & 0b11; // Flag for the scale; 0=1, 1=0.5, 2=0.25
                float scale = getScale(scaleFlag);
                traveled += scale;
                blockCount++;
                
                // Best case: we found it!
                if (traveled >= dist) {
                    blockId = hasAtlas ? mem.readByte(blocksAddr + blockCount * bytesPerBlock)
                            : mem.readShort(blocksAddr + blockCount * bytesPerBlock);
                    blockScale = scale;
                    break outer;
                }
            }
            
            // If we got this far, the block was not in first long we read
            // Just continue...
        }
        
        // TODO construct some kind of block out of id and scale
        return null;
    }
    
    private long sizesAddr() {
        return address + 1 + (hasAtlas ? DataConstants.MATERIAL_ATLAS : 0);
    }
    
    private long blocksAddr() {
        return sizesAddr() + DataConstants.BLOCK_SIZE_DATA;
    }
    
    private float getScale(long scaleFlag) {
        if (scaleFlag == 0)
            return 1f;
        else if (scaleFlag == 1)
            return 0.5f;
        else
            return 0.25f;
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
        return length + (hasAtlas ? DataConstants.CHUNK_STATIC : DataConstants.CHUNK_STATIC_NOATLAS);
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
