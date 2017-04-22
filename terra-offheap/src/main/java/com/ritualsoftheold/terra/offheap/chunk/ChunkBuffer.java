package com.ritualsoftheold.terra.offheap.chunk;

import java.io.IOException;

import org.xerial.snappy.Snappy;

import com.ritualsoftheold.terra.offheap.DataConstants;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Represents a buffer or "block" of chunks.
 *
 */
public class ChunkBuffer {
    
    private static Memory mem = OS.memory();
    
    /**
     * Maximum chunk count in this buffer.
     */
    private int chunkCount;
    
    /**
     * Array of memory addresses for chunks.
     */
    private long[] chunks;
    
    /**
     * Array of chunk data lengths.
     */
    private long[] lengths;
    
    /**
     * Index of first free chunk slot in this buffer.
     */
    private int freeIndex;
    
    /**
     * Every time memory is allocated, the amount of it to be allocated is
     * increased by this value. This is to avoid constant and pointless
     * reallocations.
     */
    private int extraAlloc;
    
    public ChunkBuffer(int chunkCount, int extraAlloc) {
        this.chunkCount = chunkCount;
        
        chunks = new long[chunkCount];
        lengths = new long[chunkCount];
        freeIndex = 0;
        this.extraAlloc = extraAlloc;
    }
    
    /**
     * Checks if this buffer can take one more chunk.
     * @return If one more chunk can be added.
     */
    public boolean hasSpace() {
        return chunkCount > freeIndex;
    }
    
    /**
     * Creates a chunk to this buffer.
     * @param firstLength Starting length of chunk (in memory). Try to have
     * something sensible here to avoid instant reallocation.
     * @return Chunk index in this buffer.
     */
    public int createChunk(int firstLength) {
        // Take index, adjust freeIndex
        int index = freeIndex;
        freeIndex++;
        
        long addr = mem.allocate(firstLength + extraAlloc);
        chunks[index] = addr;
        lengths[index] = firstLength + extraAlloc;
        
        // And finally return the index
        return index;
    }
    
    /**
     * Reallocates chunk with given amount of space. Note that you <b>must</b>
     * free old data, after you have copied all relevant parts to new area.
     * @param index Chunk index in this buffer.
     * @param newLength New length in bytes.
     * @return New memory address. Remember to free the old one!
     */
    public long reallocChunk(int index, long newLength) {
        long addr = mem.allocate(newLength + extraAlloc);
        
        chunks[index] = addr;
        lengths[index] = newLength + extraAlloc;
        
        return addr;
    }
    
    /**
     * Loads chunk buffer from given memory address
     * @param addr
     */
    public void load(long addr) {
        // Read pointer data
        for (int i = 0; i < chunkCount; i++) {
            long entry = mem.readLong(addr + i * DataConstants.CHUNK_POINTER_STORE) >>> 8;
            long chunkAddr = addr + (entry >>> 24);
            long chunkLen = (entry & 0xffffff);
            
            // Save the length
            lengths[i] = chunkLen + extraAlloc;
            
            // Allocate memory, then copy data
            long newAddr = mem.allocate(chunkLen + extraAlloc);
            mem.copyMemory(chunkAddr, newAddr, chunkLen);
            chunks[i] = newAddr;
        }
    }
    
    /**
     * Gets how much space saving this buffer would take.
     * @return Space required for saving.
     */
    public long getSaveSize() {
        long size = 0;
        for (long len : lengths) {
            size += len;
        }
        return size;
    }
    
    /**
     * Saves chunks at given address. Make sure you have enough space!
     * @param addr Memory address.
     */
    public void save(long addr) {
        int saveOffset = 0;
        for (int i = 0; i < chunkCount; i++) {
            long chunkLen = lengths[i]; // Take length of chunk
            
            // Write chunk offset and length in save data
            long curAddr = addr + i * DataConstants.CHUNK_POINTER_STORE;
            mem.writeInt(curAddr, saveOffset); // Offset
            mem.writeShort(curAddr + 4, (short) (chunkLen >>> 8)); // Length
            mem.writeByte(curAddr + 5, (byte) (chunkLen & 0xff));
            
            mem.copyMemory(chunks[i], addr + saveOffset, chunkLen); // Copy data to save location
            saveOffset += chunkLen; // Increase save offset for next chunk
        }
    }
    
    /**
     * Returns an iterator for this buffer. If amount of chunks
     * changes, iterator will become invalid and unsafe to use.
     * @return Chunk iterator.
     */
    public ChunkBufferIterator iterator() {
        return new ChunkBufferIterator(chunks, lengths, freeIndex - 1);
    }
    
    public int getExtraAlloc() {
        return extraAlloc;
    }
    
    public long getChunkAddress(int bufferId) {
        return chunks[bufferId];
    }
    
    public long getChunkLength(int bufferId) {
        return lengths[bufferId];
    }
    
    /**
     * Unpacks specific chunk to given address.
     * @param index
     * @param addr
     * @throws IOException 
     */
    public void unpack(int index, long addr) throws IOException {
        Snappy.rawUncompress(chunks[index], lengths[index], addr);
    }
    
    public void pack(int index, long addr, int length) throws IOException {
        // TODO optimize to do less memory allocations
        long tempAddr = mem.allocate(length);
        long compressedLength = Snappy.rawCompress(addr, length, tempAddr);
        if (compressedLength  > lengths[index]) { // If we do not have enough space, allocate more
            reallocChunk(index, compressedLength);
        }
        mem.copyMemory(tempAddr, chunks[index], compressedLength); // Copy temporary packed data to final destination
        mem.freeMemory(tempAddr, length); // Free temporary packing memory
    }
}
