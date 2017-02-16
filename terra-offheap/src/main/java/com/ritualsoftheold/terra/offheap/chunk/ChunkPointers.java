package com.ritualsoftheold.terra.offheap.chunk;

import com.ritualsoftheold.terra.offheap.DataConstants;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Stores the meta-pointers for chunks.
 *
 */
public class ChunkPointers {
    
    private static final Memory mem = OS.memory();
    
    private long address;
    
    private long used;
    
    private long reserved;
    
    private long reserveFactor;
    
    public ChunkPointers(long dataAddr, long length, long reserveFactor) {
        used = length;
        reserved = length + reserveFactor;
        this.reserveFactor = reserveFactor;
        
        address = mem.allocate(reserved);
        mem.copyMemory(dataAddr, address, used);
    }
    
    /**
     * Reads the chunk data. Returned format:
     * 1 byte: nothing
     * 4 bytes: chunk pointer inside this file, starts from beginning of actual chunk data
     * 3 bytes: length of chunk data
     * @param index Chunk index.
     * @return
     */
    public long readChunk(int index) {
        return mem.readLong(address + index * DataConstants.CHUNK_POINTER_STORE) >>> 8;
    }
    
    public void writeChunk(int index, long data) { // TODO test this, complex bit shifts there
        long addr = address + index * DataConstants.CHUNK_POINTER_STORE;
        mem.writeInt(addr, (int) (data >>> 24));
        mem.writeShort(addr + 4, (short) (data >>> 8 & 0xffff));
        mem.writeByte(addr + 6, (byte) (data & 0xff));
    }
    
    /**
     * Adds new chunk to data.
     * @param data Data. see {@link #readChunk(int)}
     */
    public void addChunk(long data) {
        long oldUsed = used; // Store old used data len
        used += DataConstants.CHUNK_POINTER_STORE; // Increment used by one slot
        long diff = used - reserved; // See if we have enough space
        if (diff < 0) { // Need more space...
            long oldAddr = address; // Back up old address
            long oldReserved = reserved;
            reserved = reserved + reserveFactor; // Increment reserved space
            
            address = mem.allocate(reserved); // Allocate the new space
            mem.copyMemory(oldAddr, address, oldUsed); // Copy old stuff to there
            mem.freeMemory(oldAddr, oldReserved); // Free old memory!
        }
        
        long addr = address + oldUsed;
        mem.writeInt(addr, (int) (data >>> 24));
        mem.writeShort(addr + 4, (short) (data >>> 8 & 0xffff));
        mem.writeByte(addr + 6, (byte) (data & 0xff));
    }
}
