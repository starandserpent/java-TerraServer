package com.ritualsoftheold.terra.offheap.chunk;

import com.ritualsoftheold.terra.offheap.DataConstants;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Represents a buffer or "block" of chunks.
 * TODO enlarging chunk would move others too -> BAD THING
 *
 */
public class ChunkBuffer {
    
    private static Memory mem = OS.memory();
    
    /**
     * Memory address of this chunk buffer.
     */
    private long address;
    
    /**
     * Length of this chunk's data.
     */
    private int length;
    
    /**
     * Chunk data offset (this is after metadata).
     */
    private int dataOffset;
    
    /**
     * Reserved space for this chunk.
     */
    private int reserved;
    
    /**
     * Reserved space is increased by this when length exceeds it.
     */
    private int reserveFactor;
    
    public ChunkBuffer(int reserved, int reserveFactor, int dataOffset) {
        this.reserved = reserved;
        this.dataOffset = dataOffset;
        this.reserveFactor = reserveFactor;
    }
    
    public long getChunkEntry(short index) {
        return mem.readLong(address + index * DataConstants.CHUNK_POINTER_STORE);
    }
}
