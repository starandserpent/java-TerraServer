package com.ritualsoftheold.terra.server;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Tracks memory that it allocates, but not with memory use listener.
 * Not thread safe!
 */
public class SelfTrackAllocator implements MemoryAllocator {
    
    private static final Memory mem = OS.memory();
    
    private int used;
    
    private final boolean zero;
    
    public SelfTrackAllocator(boolean zero) {
        this.zero = zero;
    }

    @Override
    public long allocate(long length) {
        used += length;
        long addr = mem.allocate(length);
        if (zero) { // Zero the memory if requested
            mem.setMemory(addr, length, (byte) 0);
        }
        return addr;
    }

    @Override
    public void free(long addr, long length) {
        mem.freeMemory(addr, length);
        used -= length;
    }

    public int getMemoryUsed() {
        return used;
    }

}
