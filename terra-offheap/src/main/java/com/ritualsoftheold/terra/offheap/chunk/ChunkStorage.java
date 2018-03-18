package com.ritualsoftheold.terra.offheap.chunk;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.offheap.io.ChunkLoader;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;
import com.ritualsoftheold.terra.offheap.node.UserOffheapChunk;

/**
 * Manages all chunks of a single world using chunk buffers.
 *
 */
public class ChunkStorage {
    
    /**
     * Array of all chunk buffers. May contain nulls for buffers which have not
     * been yet needed.
     */
    private AtomicReferenceArray<ChunkBuffer> buffers;
    
    /**
     * Lists non-closed wrappers for chunks per buffer.
     */
    private AtomicIntegerArray aliveWrappers;
    
    /**
     * Lists user counts for all chunk buffers. 0 means that the buffer is not
     * in use; anything above it means that it is not safe to unload!
     */
    private AtomicIntegerArray userCounts;
    
    /**
     * Creates chunk buffers.
     */
    private ChunkBuffer.Builder bufferBuilder;
    
    /**
     * Loads data from disk as necessary.
     */
    private ChunkLoader loader;
    
    private Executor executor;
    
    public ChunkStorage(ChunkBuffer.Builder bufferBuilder, int maxBuffers, ChunkLoader loader, Executor executor) {
        this.bufferBuilder = bufferBuilder;
        this.loader = loader;
        this.buffers = new AtomicReferenceArray<>(maxBuffers);
        this.aliveWrappers = new AtomicIntegerArray(maxBuffers);
        this.userCounts = new AtomicIntegerArray(maxBuffers);
        this.executor = executor;
    }
    
    public int newChunk() {
        boolean secondTry = true;
        while (true) {
            for (int i = 0; i < buffers.length(); i++) {
                ChunkBuffer buf = buffers.get(i);
                
                if (buf == null) { // Oh, that buffer is not loaded
                    if (secondTry) {
                        buf = loadBuffer(i); // Load it, now
                    } else { // Ignore if there is potential to not have load new buffers
                        continue;
                    }
                }
                
                buf.waitLoading(); // Make sure it is safe to use
                
                if (buf.getFreeCapacity() > 0) {
                    int index = buf.newChunk();
                    if (index != -1) { // If it succeeded
                        // Return full id for new chunk
                        return i << 16 | index;
                    }
                    // Fail means "try different buffer"
                }
            }
            
            // We failed to find free space even after loading null buffers
            if (secondTry) {
                throw new IllegalStateException("chunk buffers exhausted, cannot create new chunk");
            }
            
            secondTry = true;
            // Try again, this time with loading more buffers enabled
        }
    }

    /**
     * Loads the chunk buffer with given index.
     * @param index Index for buffer.
     * @return The buffer.
     */
    private ChunkBuffer loadBuffer(int index) {
        boolean success = createBuffer(index);
        if (!success) {
            // Someone else created the buffer
            ChunkBuffer buf = buffers.get(index);
            buf.waitLoading();
            return buf;
        }
        
        loader.loadChunks(index, buffers.get(index));
        ChunkBuffer buf = buffers.get(index);
        buf.loadingReady(); // We're done, other threads can access the buffer now
        return buf;
    }
    
    /**
     * Creates a chunk buffer and assigns to given index. Note that this
     * operation is synchronous to prevent creation of conflicting chunk
     * buffers. If it returns false, someone else created the buffer.
     * @param index Index for new buffer.
     * @return If creation succeeded.
     */
    private synchronized boolean createBuffer(int index) {
        if (buffers.get(index) != null) { // Check if already created
            return false;
        }
        
        // Create buffer
        buffers.set(index, bufferBuilder.build(index));
        
        return true;
    }
    
    /**
     * Creates temporary chunk object. Only for internal use, might mess with
     * memory manager if used in wrong place!
     * @param chunkId
     * @param materialRegistry
     * @return Chunk.
     */
    public OffheapChunk getTemporaryChunk(int chunkId, MaterialRegistry materialRegistry) {
        ChunkBuffer buf = buffers.get(chunkId >>> 16);
        return new OffheapChunk(buf, chunkId & 0xffff, materialRegistry);
    }
    
    /**
     * Gets a chunk buffer. Only for internal usage, might cause trouble
     * with memory manager if not used correctly.
     * @param index
     * @return Chunk buffer (or null).
     */
    public ChunkBuffer getBuffer(int index) {
        return buffers.get(index);
    }
    
    /**
     * Gets or loads a chunk buffer. Only for internal usage.
     * @param index
     * @return
     */
    public ChunkBuffer getOrLoadBuffer(int index) {
        ChunkBuffer buf = buffers.get(index);
        if (buf == null) { // Not available, load it
            loadBuffer(index);
            buf = buffers.get(index);
        }
        buf.waitLoading();
        return buf;
    }

    /**
     * Makes sure that a chunk with given id is loaded. It may be empty, though!
     * @param chunkId Full chunk id.
     */
    public void ensureLoaded(int chunkId) {
        int bufIndex = chunkId >>> 16;
        getOrLoadBuffer(bufIndex);
    }
    
    /**
     * Creates a managed wrapper for a chunk with given id using given material
     * registry. Chunk must be closed once it is no longer used. Storing
     * a chunk for long period of time is not recommended.
     * @param chunkId
     * @param materialRegistry
     * @return Chunk wrapper.
     */
    public OffheapChunk getChunk(int chunkId, MaterialRegistry materialRegistry) {
        int bufIndex = chunkId >>> 16;
        markUsed(bufIndex);
        ChunkBuffer buf = buffers.get(bufIndex);
        OffheapChunk wrapper = new UserOffheapChunk(buf, chunkId, materialRegistry, aliveWrappers, bufIndex);
        markUnused(bufIndex); // Must be after constructing wrapper
        // Constructor places buffer in aliveWrappers, so after it we can mark it "unused"
        return wrapper;
    }

    public AtomicReferenceArray<ChunkBuffer> getAllBuffers() {
        return buffers;
    }
    
    public int markUsed(int index) {
        return userCounts.incrementAndGet(index);
    }
    
    public int markUnused(int index) {
        return userCounts.decrementAndGet(index);
    }
    
    public int getUsedCount(int index) {
        return userCounts.get(index);
    }
    
    /**
     * Gets given block from given chunk.
     * @param chunk Chunk id.
     * @param block Block index.
     * @return Block id.
     */
    public short getBlock(int chunk, int block) {
        int bufId = chunk >>> 16;
        markUsed(bufId);
        ChunkBuffer buf = getOrLoadBuffer(bufId);
        short id = buf.getBlock(chunk, block);
        markUnused(bufId);
        
        return id;
    }
    
    /**
     * Gets blocks from given chunk.
     * @param chunk Chunk id.
     * @param blocks Block indices.
     * @param ids Array where to place ids.
     */
    public void getBlocks(int chunk, int[] blocks, short[] ids) {
        int bufId = chunk >>> 16;
        markUsed(bufId);
        ChunkBuffer buf = getOrLoadBuffer(bufId);
        buf.getBlocks(chunk, blocks, ids, 0, blocks.length);
        markUnused(bufId);
    }
    
    /**
     * Gets blocks from given chunks.
     * @param chunks Chunk ids.
     * @param blocks Block indices.
     * @param ids Array where to place ids.
     */
    public void getBlocks(int[] chunks, int[] blocks, short[] ids) {
        int chunkStart = 0; // Where chunk began
        int curChunk = chunks[0];
        
        // Go through all chunks and blocks inside them while batching queries
        for (int i = 0; i < chunks.length; i++) {
            int chunk = chunks[i];
            if (chunk != curChunk) {
                int bufId = curChunk >>> 16;
                markUsed(bufId);
                ChunkBuffer buf = getOrLoadBuffer(bufId); // Get buffer where chunk is
                buf.getBlocks(curChunk, blocks, ids, chunkStart, i + 1);
                markUnused(bufId);
                
                chunkStart = i; // Update chunk start here
                curChunk = chunk;
            }
        }
        
        if (chunkStart != chunks.length - 1) { // Still something to lookup?
            int bufId = curChunk >>> 16;
            markUsed(bufId);
            ChunkBuffer buf = getOrLoadBuffer(bufId); // Get buffer where chunk is
            buf.getBlocks(curChunk, blocks, ids, chunkStart, blocks.length);
            markUnused(bufId);
        }
    }

    public CompletableFuture<ChunkBuffer> saveBuffer(ChunkBuffer buf) {
        return CompletableFuture.supplyAsync(() -> {
            loader.saveChunks(buf.getId(), buf);
            return buf;
        }, executor);
    }

    public void unloadBuffer(int index, boolean saveFirst) {
        CompletableFuture.runAsync(() -> {
            ChunkBuffer buf = buffers.getAndSet(index, null); // Get buffer, set it to null
            // Once used mark gets to zero, we can unload
            while (true) { // Spin loop until used < 1
                // TODO Java 9 Thread.onSpinWait
                int used = getUsedCount(index);
                if (used < 1) {
                    break;
                }
            }
            while (true) { // Spin loop until aliveWrappers < 1
                int alive = aliveWrappers.get(index);
                if (alive < 1) {
                    break;
                }
            }
            
            // Proceed to unload
            if (saveFirst) {
                saveBuffer(buf).join();
            }
            
            // Finally, unload
            buf.unload();
            // And then buffer "wrapper" object is left for GC to claim
        }, executor);
    }
}
