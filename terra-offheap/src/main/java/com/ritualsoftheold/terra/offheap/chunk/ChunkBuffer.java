package com.ritualsoftheold.terra.offheap.chunk;

import java.util.concurrent.atomic.AtomicInteger;

import com.ritualsoftheold.terra.offheap.DataConstants;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Contains some chunks in memory.
 *
 */
public class ChunkBuffer {
    
    private static final Memory mem = OS.memory();
    
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
    private AtomicInteger freeIndex;
    
    /**
     * When this was last needed.
     */
    private volatile long neededTime;
    
    /**
     * Every time memory is allocated, the amount of it to be allocated is
     * increased by this value. This is to avoid constant and pointless
     * reallocations.
     */
    private int extraAlloc;
    
    /**
     * Buffer id for storage that contains this.
     * TODO what if a buffer is in multiple storages? is that allowed, actually?
     */
    private short bufferId;
    
    public ChunkBuffer(int chunkCount, int extraAlloc, short bufferId) {
        this.chunkCount = chunkCount;
        
        chunks = new long[chunkCount];
        lengths = new long[chunkCount];
        freeIndex = new AtomicInteger(0);
        this.extraAlloc = extraAlloc;
        this.bufferId = bufferId;
    }
    
    public short getId() {
        return bufferId;
    }
    
    /**
     * Checks if this buffer can take one more chunk.
     * @return If one more chunk can be added.
     */
    public boolean hasSpace() {
        return chunkCount > freeIndex.get();
    }
    
    /**
     * Creates a chunk to this buffer.
     * @param firstLength Starting length of chunk (in memory). Try to have
     * something sensible here to avoid instant reallocation.
     * @return Chunk index in this buffer.
     */
    public int createChunk(int firstLength) {
        // Take index, then adjust freeIndex
        int index = freeIndex.getAndIncrement();
        /*
         * Thread safe, I guess? First we get index+increment it in atomic
         * operation. It is not possible to enter actual creation code
         * until BOTH are done, so you can't (hopefully) overwrite existing
         * data with race conditions.
         */
        
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
     * Loads chunk buffer from given memory address which contains given amount
     * of chunks. Note that this is not allowed to exceed the amount given
     * when this buffer was created!
     * @param addr
     * @param count
     */
    public void load(long addr, int count) {
        // Read pointer data
        for (int i = 0; i < count; i++) {
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
        freeIndex.set(count);
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
    public int save(long addr) {
        int chunksLoaded = freeIndex.get(); // How many chunks this actually contains
        
        int saveOffset = 0;
        for (int i = 0; i < chunksLoaded; i++) {
            long chunkLen = lengths[i]; // Take length of chunk
            
            // Write chunk offset and length in save data
            long curAddr = addr + i * DataConstants.CHUNK_POINTER_STORE;
            mem.writeInt(curAddr, saveOffset); // Offset
            mem.writeShort(curAddr + 4, (short) (chunkLen >>> 8)); // Length
            mem.writeByte(curAddr + 5, (byte) (chunkLen & 0xff));
            
            mem.copyMemory(chunks[i], addr + saveOffset, chunkLen); // Copy data to save location
            saveOffset += chunkLen; // Increase save offset for next chunk
        }
        
        return chunksLoaded;
    }
    
    /**
     * Returns an iterator for this buffer. If amount of chunks
     * changes, iterator will become invalid and unsafe to use.
     * @return Chunk iterator.
     */
    public ChunkBufferIterator iterator() {
        return new ChunkBufferIterator(chunks, lengths, freeIndex.get() - 1);
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
     */
    public void unpack(int index, long addr) {
        RunLengthCompressor.compress(chunks[index], addr);
    }
    
    public void pack(int index, long addr, int length) {
        // TODO optimize to do less memory allocations
        long tempAddr = mem.allocate(length);
        long compressedLength = RunLengthCompressor.compress(addr, tempAddr);
        if (compressedLength  > lengths[index]) { // If we do not have enough space, allocate more
            reallocChunk(index, compressedLength);
        }
        mem.copyMemory(tempAddr, chunks[index], compressedLength); // Copy temporary packed data to final destination
        mem.freeMemory(tempAddr, length); // Free temporary packing memory
    }

    public int putChunk(long addr) {
        long tempAddr = mem.allocate(DataConstants.CHUNK_UNCOMPRESSED); // TODO optimize to allocate less memory
        int compressedLength = RunLengthCompressor.compress(addr, tempAddr);
        
        int bufferId = createChunk(compressedLength);
        mem.copyMemory(tempAddr, getChunkAddress(bufferId), compressedLength);
        return bufferId;
    }
    
    /**
     * Sets when this buffer was last needed.
     * @param time Time in milliseconds.
     */
    public void setLastNeeded(long time) {
        neededTime = time;
    }
    
    public long getLastNeeded() {
        return neededTime;
    }
    
    public int getChunkCount() {
        return freeIndex.get();
    }
}