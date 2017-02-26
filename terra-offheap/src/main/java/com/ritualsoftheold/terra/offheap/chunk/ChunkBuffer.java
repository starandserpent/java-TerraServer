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
    
    public long addChunk(OffheapChunk chunk) {
        int offset = offsets[offsetIndex]; // We can begin new chunk there
        offsetIndex++; // Increment index to point to next offset
        offsets[offsetIndex] = offset + chunk.l_getDataSize(); // Add actually offset to array where index points to
        chunks[offsetIndex] = chunk; // Store chunk
        chunk.bufferId(offsetIndex); // "buffer id" aka offset index, so chunk can request update
        
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
        } else { // This is not last chunk
            /*
             * Old length of chunk data:
             * old end of chunk data - beginning (old AND new) of chunk data
             */
            int oldLength = offsets[nextId] - offsets[chunkId];
            if (oldLength >= length) { // If we have enough space...
                // For now, do nothing, since we have enough space
                return;
            } // Or if we don't have enough space...
            int change = length - oldLength; // Calculate how much extra bytes we need
            /*
             * Loop through chunk ids. Start from next chunk's id, stop at
             * offset index, after which there is not chunk data.
             */
            for (int i = nextId; i <= offsetIndex; i++) {
                offsets[i] += change; // Update offset
                chunks[i].memoryAddress(address + offsets[i]); // Tell the chunk about address update
            }
        }
    }
    
    /**
     * Checks if this chunk buffer can contain one more chunk with given size.
     * Returns false if there can be no more chunks OR if given chunk is too
     * big to fit to the buffer.
     * @param length
     * @return If you can add chunk with given length.
     */
    public boolean hasSpace(int length) {
        if (offsetIndex == offsets.length) { // Out of index array space
            return false;
        }
        if (offsets[offsetIndex] + length > length) { // Out of space... literally
            return false;
        }
        return true;
    }
}
