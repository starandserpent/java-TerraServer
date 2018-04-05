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
        return index >> 1;
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
    public ProcessResult processQueries(OffheapChunk chunk, long queue, int size) {
        long palette = chunk.memoryAddress(); // Palette is at beginning
        long blocks = palette + 16 * 4;
        
        for (int i = 0; i < size * 8; i += 8) {
            long query = mem.readVolatileLong(queue + i); // Read the query
            int index = (int) (query >>> 16);
            int id = (int) (query & 0xffff);
            
            // Figure out correct palette id
            int paletteId = findPaletteId(palette, id);
            if (paletteId == -1) { // Palette has been exhausted
                // TODO handle failure
            }
            
            int byteIndex = byteIndex(index);
        }
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
