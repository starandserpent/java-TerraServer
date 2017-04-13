package com.ritualsoftheold.terra.offheap.chunk;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import com.ritualsoftheold.terra.offheap.io.ChunkLoader;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectArrayMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;

/**
 * Chunk storage stores chunks in their memory representation.
 *
 */
public class ChunkStorage {
    
    /**
     * Chunk buffers.
     */
    private Short2ObjectMap<ChunkBuffer> buffers;
    
    /**
     * How many chunks there can be per buffer?
     */
    private int chunksPerBuffer;
    
    /**
     * Extra alloc for chunk buffers.
     */
    private int extraAlloc;
    
    private ChunkLoader loader;
    
    private Executor loaderExecutor;
    
    /**
     * Cache of chunks, which are NOT compressed.
     */
    private Int2ObjectMap<OffheapChunk> chunkCache;
    
    /**
     * Initializes new chunk storage. Usually this should be done once per world.
     * @param loader Chunk loader, responsible for loading and optionally saving chunks.
     * @param executor Executor for asynchronous chunk loading.
     * @param chunksPerBuffer Maximum amount of chunks per chunk buffer.
     * @param extraAlloc How many bytes to allocate at end of each chunk
     * in all chunk buffers.
     */
    public ChunkStorage(ChunkLoader loader, Executor executor, int chunksPerBuffer, int extraAlloc) {
        this.loader = loader;
        loaderExecutor = executor;
        this.chunksPerBuffer = chunksPerBuffer;
        this.extraAlloc = extraAlloc;
        
        chunkCache = new Int2ObjectOpenHashMap<>();
        
        this.buffers = new Short2ObjectArrayMap<>();
    }
    
    public CompletableFuture<ChunkBuffer> requestBuffer(short bufferId) {
        ChunkBuffer buf = buffers.get(bufferId);
        if (buf == null) { // Oops we need to load this
            ChunkBuffer newBuf = new ChunkBuffer(chunksPerBuffer, extraAlloc); // Create buffer
            CompletableFuture<ChunkBuffer> future = CompletableFuture.supplyAsync(() -> loader.loadChunks(bufferId, newBuf), loaderExecutor);
            future.thenAccept((loadedBuffer) -> buffers.put(bufferId, loadedBuffer));
            return future;
        } else {
            return CompletableFuture.completedFuture(buf);
        }
    }
    
    public CompletableFuture<ChunkBuffer> saveBuffer(short bufferId, Consumer<ChunkBuffer> callback) {
        ChunkBuffer buf = buffers.get(bufferId);
        if (buf != null) {
            return CompletableFuture.supplyAsync(() -> loader.saveChunks(bufferId, buf), loaderExecutor);
        } else {
            throw new IllegalStateException("chunk buffer not even loaded!");
        }
    }
    
    public void requestChunk(int chunkId, Consumer<OffheapChunk> callback) {
        OffheapChunk chunk = chunkCache.get(chunkId);
        if (chunk == null) { // Not in cache...
            short bufferId = (short) (chunkId >>> 16);
            // TODO do this later
        }
    }
    
}
