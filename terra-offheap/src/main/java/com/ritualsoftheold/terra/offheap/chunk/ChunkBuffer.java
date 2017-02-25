package com.ritualsoftheold.terra.offheap.chunk;

import com.ritualsoftheold.terra.offheap.node.OffheapChunk;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * A kind of offheap buffer which is specially meant to store chunk data.
 *
 */
public class ChunkBuffer {
    
    private static Memory mem = OS.memory();
    
    private long address;
    
    private int length;
    
    /**
     * Offsets for chunks. Each value points to one byte after
     * chunk's end, i.e. where new one can start.
     */
    private int[] offsets;
    
    private int offsetIndex;
    
    private OffheapChunk[] chunks;
    
    public ChunkBuffer(int length, int maxChunks) {
        this.length = length;
        address = mem.allocate(length);
        offsets = new int[maxChunks];
        offsetIndex = 0;
        chunks = new OffheapChunk[maxChunks];
    }
    
    public long addChunk(OffheapChunk chunk, int length) {
        int offset = offsets[offsetIndex]; // We can begin new chunk there
        offsetIndex++; // Increment index to point to next offset
        offsets[offsetIndex] = offset + length; // Add actually offset to array where index points to
        chunks[offsetIndex] = chunk; // Store chunk
        
        long addr = address + offset;
        chunk.memoryAddress(addr); // Tell the chunk about memory address
        
        return addr; // Return where the chunk data can be written
    }
    
    /**
     * Updates length of a chunk. This might move other chunks in this buffer,
     * but they will be notified automatically.
     * @param chunkId
     * @param length
     */
    public void updateChunk(int chunkId, int length) {
        int nextId = chunkId + 1;
        if (nextId == offsetIndex) { // This is last chunk
            offsets[nextId] = offsets[chunkId] + length; // Just update this...
        } else { // This is not last chunk :(
            int oldLength = offsets[nextId] - offsets[chunkId];
            if (oldLength >= length) {
                // For now, do nothing, since we have enough space
                return;
            }
            int change = length - oldLength;
            for (int i = nextId; i < offsetIndex; i++) {
                offsets[i] += change; // Update offset
                chunks[i].memoryAddress(address + offsets[i]); // Tell the chunk about address update
            }
        }
    }
}
