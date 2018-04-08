package com.ritualsoftheold.terra.offheap.chunk;

import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.LockSupport;

import com.ritualsoftheold.terra.offheap.BuildConfig;
import com.ritualsoftheold.terra.offheap.Pointer;
import com.ritualsoftheold.terra.offheap.chunk.compress.ChunkFormat;
import com.ritualsoftheold.terra.offheap.chunk.compress.EmptyChunkFormat;
import com.ritualsoftheold.terra.offheap.memory.MemoryUseListener;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk.Storage;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Contains chunks in memory.
 *
 */
public class ChunkBuffer {
    
    private static final Memory mem = OS.memory();
    
    private AtomicReferenceArray<OffheapChunk> chunks;
    
    /**
     * How many changes can be queued per chunk.
     */
    private final int queueSize;
    
    /**
     * Address the continuous block of memory where change queues are stored.
     */
    private final @Pointer long changeQueues;
    
    /**
     * Current count of chunks in this buffer. This also serves
     * as first free index to this buffer.
     */
    private AtomicInteger chunkCount;
    
    /**
     * Maximum count of chunks in this buffer.
     */
    private final int maxCount;
    
    /**
     * If of this chunk buffer.
     */
    private final int bufferId;
    
    /**
     * Returns when this buffer was last used.
     */
    private volatile long lastNeeded;
    
    /**
     * Memory usage listener.
     */
    private MemoryUseListener memListener;
    
    /**
     * Allocates and deallocates memory for chunks on demand.
     *
     */
    public class Allocator {
        
        // TODO optimize to recycle memory
        
        /**
         * Allocates memory for a chunk with given length.
         * @param length Length of data.
         * @return Memory address where to put it.
         */
        public @Pointer long alloc(int length) {
            memListener.onAllocate(length);
            return mem.allocate(length);
        }
        
        /**
         * Frees memory from chunk data. This is rather low level utily;
         * be careful to NOT free data which may be used
         * @param address
         * @param length
         */
        public void free(@Pointer long addr, int length) {
            mem.freeMemory(addr, length);
            memListener.onFree(length);
            
        }
        
        /**
         * Creates a dummy allocator, which will try to use given address
         * if length of first allocation matches length given here.
         * @param address Address where there is free.
         * @param length Length of free space.
         */
        public Allocator createDummy(@Pointer long addr, int length) {
            return new DummyAllocator(addr, length);
        }
        
        /**
         * Sometimes chunk formats or related classes just want to pass memory
         * around; no actual allocations are necessary (or wanted). In these cases,
         * requesting a dummy allocator is best way to operate.
         *
         */
        private class DummyAllocator extends Allocator {
            
            /**
             * Address for memory this will try to provide.
             */
            private @Pointer long dummyAddr;
            
            private int dummyLength;
            
            private DummyAllocator(long addr, int length) {
                this.dummyAddr = addr;
                this.dummyLength = length;
            }
            
            public long alloc(int length) {
                if (length == dummyLength) {
                    dummyLength = -1; // Disallow further dummy allocations
                    return dummyAddr;
                } else {
                    // Get rid of dummy memory
                    // TODO recycle
                    mem.freeMemory(dummyAddr, dummyLength);
                    memListener.onFree(dummyLength);
                    return super.alloc(length); // Do real allocation
                }
            }
        }
    }
    
    private Allocator allocator;
    
    private int staticDataLength;
    
    /**
     * ChunkStorage waits on this before performing actions on the buffer.
     * Not volatile, because getting previous value just makes the wait
     * a tiny bit longer.
     */
    private boolean ready;
    
    /**
     * If we should perform ready checks on individual chunks. If false, they
     * will do nothing.
     */
    private boolean perChunkReady;
    
    public ChunkBuffer(int id, int maxChunks, int chunkQueueSize, MemoryUseListener memListener, boolean perChunkReady) {
        bufferId = id; // Set buffer id
        
        // Initialize counts (max: parameter, current: 0)
        maxCount = maxChunks;
        chunkCount = new AtomicInteger(0);
        
        // Allocate some offheap memory for chunk change queues
        queueSize = chunkQueueSize;
        int queueMemoryNeeded = 2 * 8 * queueSize; // Two queues (one for swapping), each entry is a long
        changeQueues = mem.allocate(queueMemoryNeeded);
        
        // Save ref to memory use listener and notify it
        this.memListener = memListener;
        memListener.onAllocate(changeQueues);
        
        // Initialize chunk memory allocator
        this.allocator = new Allocator();
        
        this.perChunkReady = perChunkReady;
        
        createEmptyChunks();
    }
    
    private void createEmptyChunks() {
        for (int i = 0; i < maxCount; i++) {
            chunks.set(i, new OffheapChunk(this, changeQueues + i * queueSize * 8 * 2,
                    changeQueues + i * queueSize * 8 * 2 + queueSize * 8, queueSize));
        }
    }
    
    /**
     * Creates a new chunk. Initially, it will be empty.
     * If a new chunk cannot be created, -1 will be returned.
     * @return Index for chunk in THIS BUFFER. Which is not global chunk id!
     */
    public int newChunk() {
        int index = chunkCount.getAndIncrement();
        if (index > maxCount - 1) { // No space...
            chunkCount.decrementAndGet(); // Whoops
            return -1;
        }
        
        return index;
    }
    
    /**
     * Gets free capacity of this chunk buffer. Note that since everything can
     * be asynchronous, you must also check that {@link #newChunk()} doesn't
     * return -1.
     * @return Free capacity.
     */
    public int getFreeCapacity() {
        return Math.max(0, maxCount - chunkCount.get());
    }
    
    public int getChunkCount() {
        return Math.min(chunkCount.get(), maxCount);
    }
    
    public OffheapChunk getChunk(int index) {
        return chunks.get(index);
    }
    
    /**
     * Allows building chunk buffers. One builder can create as many buffers
     * as required. Settings may be altered between building buffers, but that
     * is NOT recommended.
     *
     */
    public static class Builder {
        
        private int maxChunks;
        private int globalQueueSize;
        private int chunkQueueSize;
        private boolean perChunkReady;
        private MemoryUseListener memListener;
        
        public Builder maxChunks(int maxChunks) {
            this.maxChunks = maxChunks;
            return this;
        }
        
        public int maxChunks() {
            return maxChunks;
        }
        
        public int globaQueue() {
            return globalQueueSize;
        }
        
        public Builder queueSize(int size) {
            this.chunkQueueSize = size;
            return this;
        }
        
        public int chunkQueue() {
            return chunkQueueSize;
        }
        
        public Builder memListener(MemoryUseListener listener) {
            this.memListener = listener;
            return this;
        }
        
        public MemoryUseListener memListener() {
            return memListener;
        }
        
        public Builder perChunkReady(boolean enabled) {
            this.perChunkReady = enabled;
            return this;
        }
        
        public boolean perChunkReady() {
            return perChunkReady;
        }
        
        public ChunkBuffer build(int index) {
            return new ChunkBuffer(index, maxChunks, chunkQueueSize, memListener, perChunkReady);
        }
    }
    
    /**
     * Attempts to load given amount of chunks from data for which there is
     * provided a memory address.
     * @param address Address of data's start.
     * @param count How many chunks are in that data.
     */
    public void load(@Pointer long addr, int count) {
        for (int i = 0; i < count; i++) {
            // Read metadata
            byte type = mem.readByte(addr);
            int length = mem.readInt(addr + 1);
            addr += 5; // To actual chunk data
            
            // Copy data that needs to be copied
            // TODO reimplement loading
            
            // Note: avoid actually copying chunk contents as it is not really necessary
            
            // Increment pointer to point to next chunk
            addr += length;
        }
    }
    
    private int getContentSize() {
        int size = 0;
        
        int count = chunkCount.get();
        for (int i = 0; i < count; i++) {
            // TODO reimplement
        }
        
        return size;
    }
    
    public int getSaveSize() {
        int count = chunkCount.get();
        return getContentSize() + count * 5;
    }
    
    public void save(@Pointer long addr) {
        int count = chunkCount.get();
        for (int i = 0; i < count; i++) {
            OffheapChunk chunk = chunks.get(i);
            Storage storage = chunk.getStorage();
            
            byte type = (byte) storage.format.getChunkType();
            int len = storage.length;
            mem.writeByte(addr, type);
            mem.writeInt(addr + 1, len);
            addr += 5; // To actual data
            
            if (len != 0) {
                mem.copyMemory(storage.address, addr, len); // Copy chunk contents
                addr += len; // Address to next chunk!
            }
        }
        
        // Make sure no loads are reordered before writing this has been completely done
        // Especially copyMemory, we can't replace that with volatile writes!
        VarHandle.fullFence();
    }
    
    /**
     * Unloads all chunks and metadata. Using this buffer while it is unloading
     * or after it has been unloaded will probably crash your JVM.
     */
    public void unload() {
        int freed = 0;
        int queueMemUsed = 2 * 8 * queueSize;
        
        // Free chunk data
        int count = chunkCount.get();
        for (int i = 0; i < count; i++) {
            OffheapChunk chunk = chunks.get(i);
            Storage storage = chunk.getStorage();
            
            int len = storage.length;
            freed += len;
            long addr = storage.address;
            mem.freeMemory(addr, len);
        }
        
        // Free static data region
        mem.freeMemory(changeQueues, queueMemUsed);
        freed += queueMemUsed;
        
        memListener.onFree(freed); // Notify memory listener once for whole unload
    }

    public int getMemorySize() {
        return getContentSize() + staticDataLength;
    }

    public int getId() {
        return bufferId;
    }

    public Allocator getAllocator() {
        return allocator;
    }

    public boolean isReady() {
        return ready;
    }
    
    public void loadingReady() {
        ready = true;
    }

    public void waitLoading() {
        while (!ready) {
            // Block until ready
            Thread.onSpinWait();
        }
    }
    
    public boolean isChunkReady(int index) {
        if (perChunkReady) // Hopefully JIT will get rid of this check entirely (branch prediction)
            return chunks.get(index).getStorage().format != EmptyChunkFormat.INSTANCE;
        return true;
    }
    
    public void waitChunkReady(int index) {
        while (!isChunkReady(index)) {
            // Block until that chunk is received
            Thread.onSpinWait();
        }
    }
}
