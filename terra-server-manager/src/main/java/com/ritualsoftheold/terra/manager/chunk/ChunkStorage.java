package com.ritualsoftheold.terra.manager.chunk;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

import com.ritualsoftheold.terra.manager.io.ChunkLoaderInterface;
import com.ritualsoftheold.terra.manager.material.Registry;
import com.ritualsoftheold.terra.manager.world.OffheapLoadMarker;
import com.ritualsoftheold.terra.manager.node.OffheapChunk;

/**
 * Manages all chunks of a single world using chunk buffers.
 *
 */
public class ChunkStorage {
    
    private final Registry registry;
    
    /**
     * Array of all chunk buffers. May contain nulls for buffers which have not
     * been yet needed.
     */
    private final AtomicReferenceArray<ChunkBuffer> buffers;
    
    /**
     * Lists user counts for all chunk buffers. 0 means that the buffer is not
     * in use; anything above it means that it is not safe to unload!
     */
    private final AtomicIntegerArray userCounts;
    
    /**
     * Creates chunk buffers.
     */
    private final ChunkBuffer.Builder bufferBuilder;
    
    /**
     * Loads data from disk as necessary.
     */
    private final ChunkLoaderInterface loader;
    
    private final Executor executor;
    
    public ChunkStorage(Registry registry, ChunkBuffer.Builder bufferBuilder, int maxBuffers, ChunkLoaderInterface loader, Executor executor) {
        this.registry = registry;
        this.bufferBuilder = bufferBuilder;
        this.loader = loader;
        this.buffers = new AtomicReferenceArray<>(maxBuffers);
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
        buffers.set(index, bufferBuilder.build(this, index));
        
        return true;
    }
    
    /**
     * Gets a chunk without checking if the buffer is loaded.
     * @param chunkId
     * @return Chunk.
     */
    public OffheapChunk getChunkInternal(int chunkId) {
        ChunkBuffer buf = getBuffer(chunkId >>> 16);
        return buf.getChunk(chunkId & 0xffff);
    }
    
    /**
     * Gets a chunk buffer. Only for internal usage, might cause trouble
     * with memory manager if not used correctly.
     * @param index
     * @return Chunk buffer (or null).
     */
    public ChunkBuffer getBuffer(int index) {
        return buffers.get(index); // Does OOB check
    }
    
    /**
     * Gets or loads a chunk buffer. Only for internal usage.
     * @param index
     * @return
     */
    public ChunkBuffer getOrLoadBuffer(int index) {
        ChunkBuffer buf = buffers.get(index); // Does OOB check
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
        // Wait buffer ready, then wait chunk ready
        getOrLoadBuffer(bufIndex).waitChunkReady(chunkId & 0xffff);
    }
    
    /**
     * Makes sure that a chunk with given id is loaded and ensures that it is
     * not unloaded until {@link #markUnused(int)} for its buffer is called.
     * @param chunkId Full chunk id.
     */
    public void ensureAndKeepLoaded(int chunkId) {
        int bufIndex = chunkId >>> 16;
        markUsed(bufIndex);
        
        // Wait buffer ready, then wait chunk ready
        getOrLoadBuffer(bufIndex).waitChunkReady(chunkId & 0xffff);
    }
    
    /**
     * Creates a managed wrapper for a chunk with given id using given material
     * registry. Chunk must be closed once it is no longer used. Storing
     * a chunk for long period of time is not recommended.
     * @param chunkId
     * @return Chunk wrapper.
     */
    public OffheapChunk getChunk(int chunkId) {
        int bufIndex = chunkId >>> 16;
        markUsed(bufIndex);
        ChunkBuffer buf = buffers.get(bufIndex);
        OffheapChunk chunk = buf.getChunk(chunkId & 0xffff);
        markUnused(bufIndex);
        return chunk;
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
                Thread.onSpinWait();
                int used = getUsedCount(index);
                if (used < 1) {
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

    /**
     * Gets the chunk buffer builder that this storage uses to
     * create new buffers. Modifications to it are not recommended.
     * @return Buffer builder.
     */
    public ChunkBuffer.Builder getBufferBuilder() {
        return bufferBuilder;
    }

    public Registry getRegistry() {
        return registry;
    }
    
    public void removeLoadMarker(OffheapLoadMarker marker) {
        Map<Integer, OffheapLoadMarker.ChunkBufferUsers> buffers = marker.getChunkBuffers();
        for (int id : buffers.keySet()) {
            userCounts.getAndAdd(id, -buffers.get(id).getUserCount());
        }
    }
}
