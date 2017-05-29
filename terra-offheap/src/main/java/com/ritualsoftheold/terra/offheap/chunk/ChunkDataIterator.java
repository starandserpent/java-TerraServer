package com.ritualsoftheold.terra.offheap.chunk;

import com.ritualsoftheold.terra.offheap.DataConstants;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Iterates over raw RLE compressed chunk data in memory.
 *
 */
public class ChunkDataIterator {
    
    private static final Memory mem = OS.memory();
    
    private long addr;
    private int offset;
    
    private short material;
    private int count;
    private int blocksDone;
    
    public ChunkDataIterator(long addr) {
        this.addr = addr;
    }
    
    public short nextMaterial() {
        int data = mem.readInt(addr + offset);
        offset += 4;
        
        material = (short) (data << 16);
        count = data & 0xffff;
        blocksDone += count;
        
        return material;
    }
    
    public int getCount() {
        return count;
    }
    
    public boolean isDone() {
        return blocksDone == DataConstants.CHUNK_MAX_BLOCKS;
    }
}
