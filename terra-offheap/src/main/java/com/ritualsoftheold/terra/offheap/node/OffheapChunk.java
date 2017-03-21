package com.ritualsoftheold.terra.offheap.node;

import java.util.Objects;

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
     * Length of block data.
     */
    private int length;
    
    private OffheapWorld world;
    
    /**
     * Storage buffer for this chunk.
     */
    private ChunkBuffer buffer;
    
    /**
     * Buffer id of this chunk (some methods in storage might need this).
     */
    private int bufferId;
    
    /**
     * Determines if this chunk uses material atlas.
     * For now, always false: need to concentrate on one thing at time.
     */
    private boolean hasAtlas = false;
    
    /**
     * If this has memory address.
     */
    private boolean isValid;
    
    // Cache these, its faster
    private long sizesAddr;
    private long blocksAddr;
    private int bytesPerBlock = hasAtlas ? 1 : 2;
    
    /**
     * Constructs new offheap chunk. Usually, DO NOT call this directly.
     * @param world
     * @param buffer
     * @param bufferId
     */
    public OffheapChunk(OffheapWorld world, ChunkBuffer buffer, int bufferId) {
        this.world = world;
        this.buffer = buffer;
        this.bufferId = bufferId;
        this.address = buffer.getChunkAddress(bufferId);
        isValid = true;
        this.length = buffer.getChunkLength(bufferId);
        
        blocksAddr = blocksAddr();
        sizesAddr = sizesAddr();
    }
    
    @Override
    public short l_getMaterial(float x, float y, float z) {
        float dist = ChunkUtils.distance(x, y, z);
        System.out.println("dist: " + dist);
        float traveled = 0f;
        int offset = 0;
        
        short blockId;
        
        /*
         * We will try to figure out correct address for the "distance".
         * Since there can be 32 flags in one long, this is quite fast - I hope.
         */
        outer: while (true) {
            long scales = mem.readLong(sizesAddr); // Scale data for 32 blocks
            System.out.println("scales: " + scales);
            
            for (int i = 32; i > 0; i--) { // We are going backwards for minor performance improvement+ease of coding
                long scaleFlag = scales >>> (i * 2 - 2) & 0b11; // Flag for the scale; 0=1, 1=0.5, 2=0.25
                System.out.println("scaleFlag: " + scaleFlag);
                traveled++; // Increment traveled by one meter
                
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
                    } else {
                        /*
                         * Get decimal parts of float coordinates.
                         * This is necessary when the scale of block is not 1m.
                         */
                        float x0 = x % 1;
                        float y0 = y % 1;
                        float z0 = z % 1;
                        System.out.println("x0: " + x0 + ", y0: " + y0 + " z0: " + z0);
                        
                        int index = ChunkUtils.get025BlockIndex(x0, y0, z0);
                        System.out.println("Index: " + index);
                        blockId = hasAtlas ? mem.readByte(blocksAddr + offset + index)
                                : mem.readShort(blocksAddr + offset + index * 2);
                        System.out.println("blocksAddr: " + blocksAddr);
                        System.out.println("Readed: " + (blocksAddr + offset + index * 2));
                    }
                    break outer;
                }
                
                /*
                 * Offset change is 1 for 1m block, 8 for 8 0.5m blocks and finally,
                 * 8^2 aka 64 for 64 64 0.5m blocks.
                 * 
                 * Sadly this makes 0.25m blocks quite costly.
                 * TODO improve 0.25m block handling
                 */
                offset += getScaleOffset(scaleFlag) * bytesPerBlock; // Offset change * bytes per block
            }
            
            // If we got this far, the block was not in first long we read
            // Just continue...
        }
        
        // TODO handle atlas dereferencing (fast way to do that)
        return blockId;
    }
    
    @Override
    public Block getBlockAt(float x, float y, float z) {
        float dist = ChunkUtils.distance(x, y, z);
        System.out.println("dist: " + dist);
        float traveled = 0f;
        int offset = 0;
        
        short blockId;
        float blockScale;
        
        /*
         * We will try to figure out correct address for the "distance".
         * Since there can be 32 flags in one long, this is quite fast - I hope.
         */
        outer: while (true) {
            long scales = mem.readLong(sizesAddr); // Scale data for 32 blocks
            System.out.println("scales: " + scales);
            
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
                                : mem.readShort(blocksAddr() + offset);
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
        
        // TODO handle atlas dereferencing (fast way to do that)
        
        return new SimpleBlock(world.getMaterialRegistry().getForWorldId(blockId), blockScale);
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
        // TODO this will be needed once initial tests are passing
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
        
        // Some variables related to 1m packing
        int repack = 8;
        boolean canRepack = true;
        
        /**
         * Address to sizes data. Modified when writing sizes data.
         */
        long sizesAddr = sizesAddr();
        long sizesData = 0; // We place 32x size data there and then write it once
        int sizesCounter = 0; // When this reaches 32, we write sizesData
        
        
        /**
         * Chunk length in bytes. Starts from zero, remember to add static
         * data to this when used with memory allocations
         */
        int dataLength = 0;
        long blocksAddr = blocksAddr();
        
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
                // Note: don't increase dataLength there, we do it AFTER we are sure that
                // this block won't end up as 64 0.25m cubes
            } else {
                System.out.println("Cannot pack: " + i);
                
                // Write block data
                int blockStart = i - (8 - repack) * 8; // We might need to start from a place which we had already looped
                for (int j = 0; j < 64; j++) {
                    System.out.println("Write: " + (blocksAddr + dataLength + j * 2) + " for " + j + ": " + data[blockStart + j] + " at " + (blockStart + j));
                    mem.writeShort(blocksAddr + dataLength + j * 2, data[blockStart + j]);
                }
                
                repack = 8; // Reset repack counter for next block
                i += repack * 8; // Set i to point at next 1m block
                
                dataLength += 64; // Just wrote 64 small blocks!
                
                // Insert into sizes data
                sizesData = sizesData << 2 | 2; // 2=0.25m
                
                // Write sizes data!
                if (sizesCounter > 30) {
                    mem.writeLong(sizesAddr, sizesData);
                    sizesData = 0;
                    sizesAddr += 8;
                    sizesCounter = 0;
                }
                
                continue;
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
                        // Insert into sizes data
                        sizesData = sizesData << 2 | 0; // 0=1m
                        
                        // Write block data
                        mem.writeShort(blocksAddr + dataLength, pid1);
                        dataLength++; // 1 1m cube
                    } else {
                        // Write block data
                        int blockStart = i - 8;
                        for (int j = 0; j < 8; j++) {
                            mem.writeShort(blocksAddr + dataLength + j * 2, packed[blockStart + j]);
                        }
                        
                        dataLength += 8; // 8 0.5m cubes
                        
                        // Insert into sizes data
                        sizesData = sizesData << 2 | 1; // 1=0.5m
                    }
                    sizesCounter++;
                    
                    // Write sizes data!
                    if (sizesCounter > 30) {
                        mem.writeLong(sizesAddr, sizesData);
                        sizesData = 0;
                        sizesAddr += 8;
                        sizesCounter = 0;
                    }
                }
                
                canRepack = true; // Finally, reset this flag
            }
        }
        
        // Get more memory for this chunk, if needed
        // TODO not needed, split into places where it IS needed (look up)
        if (!isValid || dataLength > length) {
            mem.freeMemory(address, length + buffer.getExtraAlloc());
            address = buffer.reallocChunk(bufferId, length);
            length = dataLength; // Set length of this chunk to new length
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

    /**
     * @return Buffer id.
     */
    public int bufferId() {
        return bufferId;
    }

    /**
     * @param bufferId New buffer id.
     */
    public void bufferId(int bufferId) {
        this.bufferId = bufferId;
    }

    @Override
    public void l_setMaterial(float x, float y, float z, short id,
            float scale) {
        // TODO Auto-generated method stub
        
    }

}
