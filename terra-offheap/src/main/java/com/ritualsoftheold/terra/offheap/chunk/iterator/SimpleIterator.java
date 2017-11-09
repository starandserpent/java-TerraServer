package com.ritualsoftheold.terra.offheap.chunk.iterator;

import com.ritualsoftheold.terra.offheap.DataConstants;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Iterates uncompressed data, one block at time.
 *
 */
public class SimpleIterator implements ChunkIterator {
    
    private static final Memory mem = OS.memory();
    
    private long addr;
    
    private short material;
    private int blocksDone;

    private boolean done;
    
    public SimpleIterator(long addr) {
        this.addr = addr;
    }
    
    @Override
    public short nextMaterial() {
        material = mem.readShort(addr + blocksDone * 2);
        blocksDone++;
        
        if (blocksDone == DataConstants.CHUNK_MAX_BLOCKS) {
            done = true;
        }
        
        return material;
    }

    @Override
    public int getCount() {
        return 1;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public void reset() {
        done = false;
        blocksDone = 0;
    }

    @Override
    public int getOffset() {
        return blocksDone;
    }

}
