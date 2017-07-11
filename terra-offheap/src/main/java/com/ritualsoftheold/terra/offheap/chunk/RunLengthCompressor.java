package com.ritualsoftheold.terra.offheap.chunk;

import com.ritualsoftheold.terra.offheap.DataConstants;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Compresses/decompresses chunks using simple run length method.
 *
 */
public class RunLengthCompressor {
    
    private static final Memory mem = OS.memory();
    
    public static int compress(long in, long out) {
        short previous = mem.readShort(in); // Read first block in
        int count = 0;
        int outIndex = 0;
        
        for (int i = 0; i < DataConstants.CHUNK_UNCOMPRESSED; i += 2) {
            short newId = mem.readShort(in + i);
            if (previous != newId || count == Short.MAX_VALUE) { // Write what we had here
                mem.writeInt(out + outIndex, previous << 16 | count);
                outIndex += 4; // Increase outIndex to point to next data slot
                count = 1; // We got ONE new block already so reset count to one
                previous = newId; // Next time we loop, this is previous
            } else {
                count++; // Stay with same block type
            }
        }
        
        return outIndex + 1;
    }
    
    public static void decompress(long in, long out) {
        int outIndex = 0;
        
        for (int i = 0; outIndex < DataConstants.CHUNK_UNCOMPRESSED; i += 4) {
            int entry = mem.readInt(in + i); // Read entry (2 bytes id+2 bytes length)
            short id = (short) (entry >>> 16);
            int count = entry & 0xffff;
            
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
