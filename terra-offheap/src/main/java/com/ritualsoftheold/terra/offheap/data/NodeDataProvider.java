package com.ritualsoftheold.terra.offheap.data;

import com.ritualsoftheold.terra.offheap.octree.OctreeStorage;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

public class NodeDataProvider implements WorldDataProvider {
    
    private static final Memory mem = OS.memory();
    
    private OctreeStorage storage;
    
    @Override
    public int newId() {
        return storage.newOctree();
    }

    @Override
    public void write(int id, int offset, short[] data) {
        short block = data[0]; // This provider supports only one material
        long addr = storage.getOctreeAddr(id);
        
        // Write block id
        mem.writeVolatileInt(addr + 1 + offset * 4, block);
        
        // Update flags
        byte flags = mem.readVolatileByte(addr); // Get flags
        byte mask = (byte) ~(1 << offset); // Calculate mask, for example 0b11101111 offset 4
        flags &= mask; // Use mask to set specific bit to zero
        
        // TODO handle potential race condition
        mem.writeVolatileByte(addr, flags);
    }

    @Override
    public void read(int id, int offset, short[] data) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isOctree() {
        return true;
    }

}
