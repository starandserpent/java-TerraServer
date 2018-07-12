package com.ritualsoftheold.terra.offheap.chunk;

import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

import com.ritualsoftheold.terra.offheap.Pointer;
import com.ritualsoftheold.terra.offheap.chunk.compress.ChunkFormat;
import com.ritualsoftheold.terra.offheap.chunk.compress.EmptyChunkFormat;
import com.ritualsoftheold.terra.offheap.data.MemoryAllocator;
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
    
    private final ChunkStorage storage;
    
    private final AtomicReferenceArray<OffheapChunk> chunks;
    
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
    private final AtomicInteger chunkCount;
    
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
    private final MemoryUseListener memListener;
    
    /**
     * Allocates and deallocates memory for chunks on demand.
     *
     */
    public class Allocator implements MemoryAllocator {
        
        // TODO optimize to recycle memory
        
        @Override
        public @Pointer long alloc(int length) {
            memListener.onAllocate(length);
            return mem.allocate(length);
        }
        
        @Override
        public void free(@Pointer long addr, int length) {
            mem.freeMemory(addr, length);
            memListener.onFree(length);
            
        }
        
        @Override
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
    
    private final Allocator allocator;
    
    private volatile int staticDataLength;
    
    /**
     * ChunkStorage waits on this before performing actions on the buffer.
     */
    private volatile boolean ready;
    
    /**
     * If we should perform ready checks on individual chunks. If false, they
     * will do nothing.
     */
    private final boolean perChunkReady;
    
    public ChunkBuffer(ChunkStorage storage, int id, int maxChunks, int chunkQueueSize, MemoryUseListener memListener, boolean perChunkReady) {
        Objects.requireNonNull(storage);
        Objects.checkIndex(id, storage.getAllBuffers().length());
        Objects.checkIndex(maxChunks, Integer.MAX_VALUE);
        Objects.checkIndex(chunkQueueSize, Integer.MAX_VALUE);
        Objects.requireNonNull(memListener);
        
        this.storage = storage;
        chunks = new AtomicReferenceArray<>(maxChunks);
        
        bufferId = id; // Set buffer id
        
        // Initialize counts (max: parameter, current: 0)
        maxCount = maxChunks;
        chunkCount = new AtomicInteger(0);
        
        // Allocate some offheap memory for chunk change queues
        queueSize = chunkQueueSize;
        int queueMemoryNeeded = 2 * 8 * queueSize * maxChunks; // Two queues (one for swapping), each entry is a long
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
            chunks.set(i, new OffheapChunk(i, this, changeQueues + i * queueSize * 8 * 2,
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
        
        public ChunkBuffer build(ChunkStorage storage, int index) {
            return new ChunkBuffer(storage, index, maxChunks, chunkQueueSize, memListener, perChunkReady);
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
            
            // Copy chunk data
            // Can't use the data in buffer, partial freeing of allocated memory is not safe
            long copyAddr = allocator.alloc(length);
            mem.copyMemory(addr, copyAddr, length);
            
            // Create storage and apply it to chunk
            Storage storage = new Storage(ChunkFormat.forType(type), copyAddr, length);
            OffheapChunk chunk = chunks.get(i);
            chunk.setStorageInternal(storage);
            
            // Increment pointer to point to next chunk
            addr += length;
        }
    }
    
    private int getContentSize() {
        int size = 0;
        
        int count = chunkCount.get();
        for (int i = 0; i < count; i++) {
            size += chunks.get(i).getStorageInternal().length;
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
            try (Storage storage = chunk.getStorage()) {
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
        }
        
        // Make sure no loads are reordered before writing this has been completely done
        // Especially copyMemory, we can't replace that with volatile/release writes!
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
            Storage storage = chunk.getStorageInternal(); // Memory manager usually CALLS this...
            while (storage.getUserCount() > 0) { // Wait for all users to give up the storage
                // (to avoid segfaults and other nasty stuff)
                Thread.onSpinWait();
            }
            
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
            return chunks.get(index).getStorageInternal().format != EmptyChunkFormat.INSTANCE;
        return true;
    }
    
    public void waitChunkReady(int index) {
        while (!isChunkReady(index)) {
            // Block until that chunk is received
            System.out.println("spinning");
            Thread.onSpinWait();
        }
    }

    public ChunkStorage getStorage() {
        return storage;
    }
}
