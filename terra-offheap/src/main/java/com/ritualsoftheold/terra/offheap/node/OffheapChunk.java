package com.ritualsoftheold.terra.offheap.node;

import java.util.Objects;

import com.ritualsoftheold.terra.node.Block;
import com.ritualsoftheold.terra.node.Chunk;
import com.ritualsoftheold.terra.offheap.ChunkUtils;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.ChunkStorage;
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
     * Length of block data.
     */
    private int length;
    
    
    private int reserved;
    
    /**
     * Storage for this chunk. Will be used to allocate memory as needed.
     */
    private ChunkStorage storage;
    
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
    
    /**
     * If this has memory address.
     */
    private boolean isValid;
    
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
                        blockId = hasAtlas ? mem.readByte(blocksAddr + offset)
                                : mem.readShort(blocksAddr + offset);
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
                        blockId = hasAtlas ? mem.readByte(blocksAddr + offset + index)
                                : mem.readShort(blocksAddr + offset + index * 2);
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
                        blockId = hasAtlas ? mem.readByte(blocksAddr + offset + index)
                                : mem.readShort(blocksAddr + offset + index * 2);
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
        return DataConstants.CHUNK_MAX_BLOCKS;
    }

    @Override
    public void getData(short[] data) {
        
    }

    @Override
    public void setData(short[] data) {
        Objects.requireNonNull(data, "chunk data array cannot be null");
        if (data.length != getMaxBlockCount()) {
            throw new IllegalArgumentException("length of data array must be exactly " + getMaxBlockCount());
        }
        
        /**
         * Packed 0.5m blocks go here.
         */
        short[] packed = new short[data.length / 8];
        
        /**
         * Packed 1m blocks go here.
         */
        short[] bigBlocks = new short[packed.length / 8];
        
        // Some variables related to 1m packing
        int repack = 8;
        boolean canRepack = true;
        
        /**
         * Block data length in bytes.
         */
        int dataLength = 0;
        
        // Loop through the data, packing 0.25m cubes to 0.5m cubes where possible
        for (int i = 0; i < data.length; i += 8) {
            // First, get 8 0.25m blocks
            short id1 = data[i];
            short id2 = data[i + 1];
            short id3 = data[i + 2];
            short id4 = data[i + 3];
            short id5 = data[i + 4];
            short id6 = data[i + 5];
            short id7 = data[i + 6];
            short id8 = data[i + 7];
            
            /*
             * Now, check if they are all same material.
             * If yes, we found 0.5m block!
             */
            if (id1 == id2 && id1 == id3 && id1 == id4 && id1 == id5 && id1 == id6 && id1 == id7 && id1 == id8) {
                packed[i / 8] = id1; // Mark id here, we packed it into 0.5m block!
            } else {
                canRepack = false; // Don't even bother trying to pack into 1m block if there are 0.25m blocks
                dataLength += 8; // 8 0.25m blocks
            }
            
            /*
             * Check for repacking, aka packing 0.5m blocks which have same material
             * to 1m blocks.
             */
            repack--; // Reduce the counter...
            if (repack < 1) { // Time to try packing!
                repack = 8; // Reset the counter
                
                if (canRepack) { // If we can't pack, don't bother to do advanced checks
                    /**
                     * Index of 0.5m packed block which is first of this
                     * batch of 8 packed blocks.
                     */
                    int pi = i / 8 - 7; // pi=packedIndex
                    
                    /*
                     * Ids of the packed blocks (pid=packedId).
                     * 0 means that we couldn't pack something.
                     */
                    short pid1 = packed[pi];
                    short pid2 = packed[pi + 1];
                    short pid3 = packed[pi + 2];
                    short pid4 = packed[pi + 3];
                    short pid5 = packed[pi + 4];
                    short pid6 = packed[pi + 5];
                    short pid7 = packed[pi + 6];
                    short pid8 = packed[pi + 7];
                    
                    if (pid1 == pid2 && pid1 == pid3 && pid1 == pid4 && pid1 == pid5 && pid1 == pid6 && pid1 == pid7 && pid1 == pid8) {
                        bigBlocks[pi / 8] = pid1; // Mark id here, we packed it into 0.5m block!
                        dataLength++; // 1 1m cube
                    } else {
                        dataLength += 8; // 8 0.5m cubes
                    }
                }
                
                canRepack = true; // Finally, reset this flag
            }
        }
        
        // Allocate memory, if necessary
        if (!isValid || dataLength > length) {
            storage.alloc(this, dataLength);
        }
        
        // Finally, construct sizes data and actual block data
        for (int i = 0; i < bigBlocks.length; i++) {
            // TODO
        }
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
        isValid = true;
        address = addr;
    }
    
    @Override
    public boolean isValid() {
        return isValid;
    }

    @Override
    public void invalidate() {
        isValid = false;
    }

}
