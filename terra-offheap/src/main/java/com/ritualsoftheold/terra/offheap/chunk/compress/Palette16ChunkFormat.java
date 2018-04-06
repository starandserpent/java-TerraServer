package com.ritualsoftheold.terra.offheap.chunk.compress;

import com.ritualsoftheold.terra.offheap.Pointer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer.Allocator;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * 4 bits per block. Max 16 block types in a chunk.
 *
 */
public class Palette16ChunkFormat implements ChunkFormat {
    
    private static final Memory mem = OS.memory();
    
    /**
     * Gets index of the byte where given index
     * @param index
     * @return
     */
    private int byteIndex(int index) {
        return index >> 1; // Quick integer divide positive by 2
    }
    
    /**
     * Gets the bit offset in the byte towards which the value should be
     * shifted from right to left.
     * @param index
     * @return
     */
    private int shiftOffset(int index) {
        // Quick modulo positive by 2
        // Then some normal math
        return ((index & 1) - 1) * -4;
    }
    
    /**
     * Finds or create a palette id.
     * @param palette Palette address.
     * @param id World block id.
     * @return Palette id or -1 if palette is exhausted.
     */
    private int findPaletteId(@Pointer long palette, int id) {
        for (int i = 0; i < 16; i++) {
            int worldId = mem.readVolatileInt(palette + i * 4);
            if (worldId == id) { // Found our palette id!
                return i;
            }
            
            if (worldId == 0) { // Previous one was last id
                // Allocate new palette id
                mem.writeVolatileInt(palette + i * 4, id);
                return i;
            }
        }
        
        // If we get this far, palette is exhausted
        return -1; // Must change chunk type
    }
    
    @Override
    public boolean convert(long from, long to, int type) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public OffheapChunk.Storage processQueries(OffheapChunk chunk, OffheapChunk.ChangeIterator changes) {
        long palette = chunk.memoryAddress(); // Palette is at beginning
        long blocks = palette + 16 * 4;
        
        while (changes.hasNext()) {
            changes.next();
            int index = changes.getIndex();
            int id = changes.getBlockId();
            
            // Figure out correct palette id
            int paletteId = findPaletteId(palette, id);
            if (paletteId == -1) { // Palette has been exhausted
                // TODO handle failure
            }
            
            int byteIndex = byteIndex(index);
            int bitOffset = shiftOffset(index);
            
            long blockAddr = blocks + byteIndex;
            int value = mem.readVolatileByte(blockAddr);
            
            /* Clearing a half of byte without branches!
             * 
             * When no shifting is done (bitOffset==0), the first 4
             * bits are AND'd with 0xf and second 0x0, thus clearing
             * the second 4 bits.
             * 
             * When shifting is done by 4, first shift will rearrange
             * the byte in int in a way that the reverse is true.
             * first 4 bits and AND'd with 0x0 and second with 0x0.
             * The second shift will then arrange them back.
             */
            // Clear the part of byte where we will write
            value = value << bitOffset & 0xfffff0f0 >>> bitOffset;
            
            // Write the 4 byte palette id to first 4 bytes or last 4 bytes
            value = value | (paletteId << bitOffset);
            
            // Write our new value in place of old one
            // One block was changed, one not
            mem.writeVolatileByte(blockAddr, (byte) value);
        }
        
        return null;
    }

    @Override
    public void getBlocks(long chunk, int[] indices, short[] ids,
            int beginIndex, int endIndex) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public SetAllResult setAllBlocks(short[] data, Allocator allocator) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getChunkType() {
        // TODO Auto-generated method stub
        return 0;
    }

}
