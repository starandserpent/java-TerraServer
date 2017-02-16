package com.ritualsoftheold.terra.offheap.data;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * A region that represents a portion of offheap memory and objects that
 * require access to that memory.
 *
 */
public class MemoryRegion {
    
    private static Memory mem = OS.memory();
    
    /**
     * Start address.
     */
    private long address;
    
    /**
     * Length of data in bytes.
     */
    private long length;
    
    private Set<OffheapObject> objects = Collections.newSetFromMap(new WeakHashMap<>());
    
    MemoryRegion(long length) {
        address = mem.allocate(length);
        this.length = length;
    }
    
    /**
     * Makes this region track certain object.
     * @param oo Object.
     */
    public void trackObject(OffheapObject oo) {
        objects.add(oo);
    }
    
    /**
     * Makes this region to stop tracking given object.
     * @param oo
     */
    public void untrackObject(OffheapObject oo) {
        objects.remove(oo);
    }
    
    /**
     * Forces this region to reallocate itself.
     */
    public void reallocate() {
        long oldAddr = address;
        address = mem.allocate(length);
        
        mem.copyMemory(oldAddr, address, length); // Copy all data there
        mem.freeMemory(oldAddr, length); // Free old location
        
        updateTracked(address - oldAddr);
    }
    
    public long memoryAddress() {
        return address;
    }
    
    /**
     * Sets memory address of this region. Make sure that there is ENOUGH SPACE,
     * otherwise it will might just segfault...
     * @param addr
     */
    public void memoryAddress(long addr) {
        long oldAddr = address;
        address = addr;
        
        mem.copyMemory(oldAddr, address, length);
        mem.freeMemory(oldAddr, length);
        
        updateTracked(address - oldAddr);
    }
    
    public long length() {
        return length;
    }
    
    private void updateTracked(long change) {
        for (OffheapObject oo : objects) {
            oo.memoryAddress(oo.memoryAddress() + change);
        }
    }
}
