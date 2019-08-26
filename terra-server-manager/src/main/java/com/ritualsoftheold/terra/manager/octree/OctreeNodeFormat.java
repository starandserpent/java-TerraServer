package com.ritualsoftheold.terra.manager.octree;

import com.ritualsoftheold.terra.manager.data.WorldDataFormat;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Handles octree world data.
 * 
 */
public class OctreeNodeFormat implements WorldDataFormat {
    
    private static final Memory mem = OS.memory();
    
    public byte getFlags(long addr) {
        return mem.readVolatileByte(addr);
    }
    
    public void modifyFlag(long addr, int index, int mod) {
        int or = mod << index;
        int and = ~or;
        while (true) {
            /*
             * Java doesn't have CAS for bytes, so we must get first int and
             * just not modify anything but given bit in first 8 bytes.
             * 
             * Also, since x86 is little endian while Java is big endian, we
             * must consider LAST 8 bits to be the real first. Confusing, huh?
             */
            int part = mem.readVolatileInt(addr); // Get the part
            
            // Make requested bit to be given value
            int modified;
            if (mod == 0) {
                modified = part & and;
            } else {
                modified = part | or;
            }
            
            // Try write the change
            if (mem.compareAndSwapInt(addr, part, modified)) {
                // Only if it succeeds, we can exit the loop
                break;
            }
        }
    }
    
    public int getNode(long addr, int index) {
        assert index >= 0 && index <= 7;
        return mem.readVolatileInt(addr + index * 4);
    }
    
    public void setNode(long addr, int index, int value) {
        assert index >= 0 && index <= 7;
        mem.writeVolatileInt(addr + index * 4, value);
    }
    
    @Override
    public boolean isOctree() {
        return true;
    }
}
