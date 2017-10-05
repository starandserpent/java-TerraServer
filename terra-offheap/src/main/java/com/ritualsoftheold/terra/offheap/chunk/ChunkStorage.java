package com.ritualsoftheold.terra.offheap.chunk;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicIntegerArray;

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
    private ChunkBuffer[] buffers;
    
    private AtomicIntegerArray aliveWrappers;
    
    private AtomicIntegerArray unloadStaged;
    
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
        this.buffers = new ChunkBuffer[maxBuffers];
        this.aliveWrappers = new AtomicIntegerArray(maxBuffers);
        this.executor = executor;
    }
    
    public int newChunk() {
        boolean secondTry = true;
        while (true) {
            for (int i = 0; i < buffers.length; i++) {
                ChunkBuffer buf = buffers[i];
                
                if (buf == null) { // Oh, that buffer is not loaded
                    if (secondTry) {
                        loadBuffer(i); // Load it, now
                    } else { // Ignore if there is potential to not have load anything
                        continue;
                    }
                }
                
                if (buf.getFreeCapacity() > 0) {
                    int index = buf.newChunk();
                    if (index != -1) { // If it succeeded
                        // Return full id for new chunk
                        return i << 16 & index;
                    }
                    // Fail means "try different buffer"
                }
            }
            
            // We failed to find free space even after loading null buffers
            if (secondTry) {
                throw new IllegalStateException("cannot create a new chunk");
            }
            
            secondTry = true;
            // ... until success
        }
    }

    /**
     * Loads given chunk buffer.
     * @param index Index for buffer.
     */
    private void loadBuffer(int index) {
        boolean success = createBuffer(index);
        if (!success) {
            return; // Someone else is loading the buffer
        }
        
        ChunkBuffer buf = buffers[index];
    }
    
    /**
     * Creates a chunk buffer and assigns to given index. Note that this
     * operation is synchronous to prevent creation of conflicting chunk
     * buffers. If it returns true,
     * @param index Index for new buffer.
     * @return If creation succeeded.
     */
    private synchronized boolean createBuffer(int index) {
        if (buffers[index] != null) { // Check if already created
            return false;
        }
        
        // Create buffer
        buffers[index] = bufferBuilder.build();
        
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
        ChunkBuffer buf = buffers[chunkId >>> 16];
        return new OffheapChunk(buf, chunkId & 0xffff, materialRegistry);
    }
    
    /**
     * Gets a chunk buffer. Only for internal usage, might cause trouble
     * with memory manager if not used correctly.
     * @param index
     * @return Chunk buffer (or null).
     */
    public ChunkBuffer getBuffer(int index) {
        return buffers[index];
    }
    
    /**
     * Gets or loads a chunk buffer. Only for internal usage.
     * @param index
     * @return
     */
    public ChunkBuffer getOrLoadBuffer(int index) {
        ChunkBuffer buf = buffers[index];
        if (buf == null) { // Not available, load it
            loadBuffer(index);
        }
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
        ChunkBuffer buf = buffers[bufIndex];
        return new UserOffheapChunk(buf, chunkId, materialRegistry, aliveWrappers, bufIndex);
    }

    public ChunkBuffer[] getAllBuffers() {
        return buffers.clone();
    }
}
