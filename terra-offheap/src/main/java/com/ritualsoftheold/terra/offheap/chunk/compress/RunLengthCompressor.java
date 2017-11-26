package com.ritualsoftheold.terra.offheap.chunk.compress;

import com.ritualsoftheold.terra.offheap.DataConstants;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Compresses/decompresses chunks using simple run length method.
 *
 */
public class RunLengthCompressor {
    
    private static final Memory mem = OS.memory();
    static final int MAX_COUNT = Character.MAX_VALUE + 1;
    
    public static int compress(long in, long out, int quota) {
        short previous = mem.readShort(in); // Read first block in
        int count = 0;
        int outIndex = 0;
        
        for (int i = 0; i < DataConstants.CHUNK_UNCOMPRESSED; i += 2) {
            // Oops. Out of space...
            if (outIndex == quota) {
                return -1;
            }
            
            short newId = mem.readShort(in + i);
            if (previous != newId || count == MAX_COUNT) { // Write what we had here
                mem.writeInt(out + outIndex, (count - 1) << 16 | previous);
                outIndex += 4; // Increase outIndex to point to next data slot
                count = 1; // We got ONE new block already so reset count to one
                previous = newId; // Next time we loop, this is previous
            } else {
                count++; // Stay with same block type
            }
        }
        if (count != 0) {
            mem.writeInt(out + outIndex, (count - 1) << 16 | previous);
            outIndex += 4; // Increase outIndex to point to next data slot
        }
        
        return outIndex;
    }
    
    public static void decompress(long in, long out) {
        int outIndex = 0;
        
        for (int i = 0; outIndex < DataConstants.CHUNK_UNCOMPRESSED; i += 4) {
            int entry = mem.readInt(in + i); // Read entry (2 bytes id+2 bytes length)
            short id = (short) (entry & 0xffff);
            int count = Short.toUnsignedInt((short) (entry >>> 16)) + 1; // I hate endianness issues
            
            for (int j = 0; j < count; j++) {
                mem.writeShort(out + outIndex + j * 2, id);
            }
            outIndex += count * 2; // Increase out pos tracker
        }
    }
    
    public static void modify(long data) {
        // TODO attempt to modify compressed chunk data WITHOUT decompress/recompress
    }
}
