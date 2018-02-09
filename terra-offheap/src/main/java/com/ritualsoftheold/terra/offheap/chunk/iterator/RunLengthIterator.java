package com.ritualsoftheold.terra.offheap.chunk.iterator;

import com.ritualsoftheold.terra.offheap.DataConstants;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Iterates over raw RLE compressed chunk data in memory.
 *
 */
public class RunLengthIterator implements ChunkIterator {
    
    private static final Memory mem = OS.memory();
    
    private long addr;
    private int offset;
    
    private short material;
    private int count;
    private int blocksDone;

    private boolean done;
    
    public RunLengthIterator(long addr) {
        this.addr = addr;
    }
    
    @Override
    public short nextMaterial() {
        material = mem.readShort(addr + offset); // TODO improve performance by reading one value
        count = Short.toUnsignedInt(mem.readShort(addr + offset + 2)) + 1; // .. without having little/big endian mess, please
        blocksDone += count;
        
        // Increase offset AFTER we used it
        offset += 4;
        
        if (blocksDone >= DataConstants.CHUNK_MAX_BLOCKS) {
            done = true;
        }
        
        return material;
    }
    
    @Override
    public int getCount() {
        return count;
    }
    
    @Override
    public boolean isDone() {
        return done;
    }
    
    @Override
    public void reset() {
        offset = 0;
        done = false;
        blocksDone = 0;
    }

    @Override
    public int getOffset() {
        return blocksDone;
    }
}
