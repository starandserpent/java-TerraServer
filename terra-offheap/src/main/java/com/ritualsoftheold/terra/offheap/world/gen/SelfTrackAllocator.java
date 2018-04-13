package com.ritualsoftheold.terra.offheap.world.gen;

import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer.Allocator;
import com.ritualsoftheold.terra.offheap.data.MemoryAllocator;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Tracks memory that it allocates, but not with memory use listener.
 * Not thread safe!
 *
 */
public class SelfTrackAllocator implements MemoryAllocator {
    
    private static final Memory mem = OS.memory();
    
    private int used;
    
    @Override
    public long alloc(int length) {
        used += length;
        return mem.allocate(length);
    }

    @Override
    public void free(long addr, int length) {
        mem.freeMemory(addr, length);
        used -= length;
    }

    @Override
    public Allocator createDummy(long addr, int length) {
        throw new UnsupportedOperationException();
    }
    
    public int getMemoryUsed() {
        return used;
    }

}
