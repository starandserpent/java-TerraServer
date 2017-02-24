package com.ritualsoftheold.terra.offheap.node;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.node.Block;
import com.ritualsoftheold.terra.node.Chunk;
import com.ritualsoftheold.terra.offheap.ChunkUtils;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.data.MemoryRegion;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;

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
    
    /**
     * World of this chunk. Used for misc operations, like force loading its
     * underlying data if not present.
     */
    private OffheapWorld world;
    
    // Cache these, its faster
    private long sizesAddr = sizesAddr();
    private long blocksAddr = blocksAddr();
    private int bytesPerBlock = hasAtlas ? 1 : 2;
    
    public OffheapChunk(OffheapWorld world, long address, int length, int reserved) {
        this.world = world;
        this.address = address;
        this.reserved = reserved;
    }
    
    @Override
    public Block getBlockAt(float x, float y, float z) {
        float dist = ChunkUtils.distance(x, y, z);
        float traveled = 0f;
        int offset = 0;
        
        short blockId;
        float blockScale;
        
        /*
         * We will try to figure out correct address for the "distance".
         * Since there can be 32 flags in one long, this is quite fast - I hope.
         */
        outer: while (true) {
            long scales = mem.readLong(sizesAddr()); // Scale data for 32 blocks
            
            for (int i = 32; i > 0; i--) { // We are going backwards for minor performance improvement+ease of coding
                long scaleFlag = scales >>> (i * 2) & 0b11; // Flag for the scale; 0=1, 1=0.5, 2=0.25
                traveled++; // Increment traveled by one meter
                /*
                 * Offset change is 1 for 1m block, 8 for 8 0.5m blocks and finally,
                 * 8^2 aka 64 for 64 64 0.5m blocks.
                 * 
                 * Sadly this makes 0.25m blocks quite costly.
                 * TODO improve 0.25m block handling
                 */
                offset += getScaleOffset(scaleFlag) * bytesPerBlock; // Offset change * bytes per block
                
                // Now, check if we have traveled long enough to have gone past the block
                if (traveled >= dist) {
                    /*
                     * Yes? Then, based on block type we will decide what to do.
                     * 
                     * 1m: Just get the block at offset.
                     * 
                     * 0.5m: First, lookup (=black magic) for the block index.
                     * Then, offset + that index read.
                     * 
                     * 0.25m: Same as above, except the lookup table is really
                     * black magic this time, having 64 possible indexes to return.
                     */
                    if (scaleFlag == 0) { // 1m cube
                        blockId = hasAtlas ? mem.readByte(offset)
                                : mem.readShort(offset);
                        blockScale = 1;
                    } else if (scaleFlag == 1) { // 0.5m cube!
                        /*
                         * Get decimal parts of float coordinates.
                         * This is necessary when the scale of block is not 1m.
                         */
                        float x0 = x % 1;
                        float y0 = y % 1;
                        float z0 = z % 1;
                        
                        int index = ChunkUtils.getSmallBlockIndex(x0, y0, z0);
                        blockId = hasAtlas ? mem.readByte(offset + index)
                                : mem.readShort(offset + index * 2);
                        blockScale = 0.5f;
                    } else {
                        /*
                         * Get decimal parts of float coordinates.
                         * This is necessary when the scale of block is not 1m.
                         */
                        float x0 = x % 1;
                        float y0 = y % 1;
                        float z0 = z % 1;
                        
                        // TODO implement method that we call here
                        int index = ChunkUtils.get025BlockIndex(x0, y0, z0);
                        blockId = hasAtlas ? mem.readByte(offset + index)
                                : mem.readShort(offset + index * 2);
                        blockScale = 0.25f;
                    }
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
    
    // Not used -  for now
    @SuppressWarnings("unused")
    private float getScale(long scaleFlag) {
        if (scaleFlag == 0)
            return 1f;
        else if (scaleFlag == 1)
            return 0.5f;
        else
            return 0.25f;
    }
    
    private int getScaleOffset(long scaleFlag) {
        if (scaleFlag == 0)
            return 1;
        else if (scaleFlag == 1)
            return 8;
        else
            return 64;
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
