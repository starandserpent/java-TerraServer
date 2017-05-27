package com.ritualsoftheold.terra.offheap.chunk;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.node.Chunk;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.io.ChunkLoader;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectArrayMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Chunk storage stores chunks in their memory representation.
 *
 */
public class ChunkStorage {
    
    private static Memory mem = OS.memory();
    
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
    
    private ChunkBuffer freeBuffer;
    private short freeBufferId;
    
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
    
    public ChunkBuffer getBuffer(short bufferId) {
        ChunkBuffer buf = buffers.get(bufferId);
        if (buf == null) { // Oops we need to load this
            buf = new ChunkBuffer(chunksPerBuffer, extraAlloc); // Create buffer
            loader.loadChunks(bufferId, buf);
            buffers.put(bufferId, buf);
        }
        return buf;
    }
    
    public CompletableFuture<ChunkBuffer> saveBuffer(short bufferId, Consumer<ChunkBuffer> callback) {
        ChunkBuffer buf = buffers.get(bufferId);
        if (buf != null) {
            return CompletableFuture.supplyAsync(() -> loader.saveChunks(bufferId, buf), loaderExecutor);
        } else {
            throw new IllegalStateException("chunk buffer not even loaded!");
        }
    }
    
    public CompletableFuture<Chunk> requestChunk(int chunkId, MaterialRegistry registry) {
        OffheapChunk chunk = chunkCache.get(chunkId);
        if (chunk == null) { // Not in cache...
            // TODO optimize memory allocations here
            // We have static-sized cache, why not recycle memory?
            short bufferId = (short) (chunkId >>> 16);
            int index = chunkId & 0xffff;
            CompletableFuture<ChunkBuffer> bufFuture = requestBuffer(bufferId);
            CompletableFuture<Chunk> chunkFuture = CompletableFuture.supplyAsync(() -> {
                long addr = mem.allocate(DataConstants.CHUNK_UNCOMPRESSED);
                ChunkBuffer buf = bufFuture.join(); // Wait for the other future
                
                // Check if this chunk was generated...
                // TODO
                
                buf.unpack(index, addr); // Unpack data
                OffheapChunk loadedChunk = new OffheapChunk(registry);
                loadedChunk.memoryAddress(addr); // Set memory address to point to data
                chunkCache.put(chunkId, loadedChunk); // Cache what we just loaded
                
                return loadedChunk;
            });
            return chunkFuture;
        } else { // Cache found!
            return CompletableFuture.completedFuture(chunk);
        }
    }
    
    public Chunk getChunk(int chunkId, MaterialRegistry registry) {
        OffheapChunk chunk = chunkCache.get(chunkId);
        if (chunk == null) { // Not in cache...
            // TODO optimize memory allocations here
            // We have static-sized cache, why not recycle memory?
            short bufferId = (short) (chunkId >>> 16);
            int index = chunkId & 0xffff;
            ChunkBuffer buf = getBuffer(bufferId);
            long addr = mem.allocate(DataConstants.CHUNK_UNCOMPRESSED);
            
            // Check if this chunk was generated...
            // TODO
            
            buf.unpack(index, addr); // Unpack data
            chunk = new OffheapChunk(registry);
            chunk.memoryAddress(addr); // Set memory address to point to data
            chunkCache.put(chunkId, chunk); // Cache what we just loaded
        }
        return chunk;
    }
    
    private ChunkBuffer findFreeBuffer() {
        for (Short2ObjectMap.Entry<ChunkBuffer> entry : buffers.short2ObjectEntrySet()) {
            ChunkBuffer buf = entry.getValue();
            if (buf.hasSpace()) {
                freeBuffer = buf;
                freeBufferId = entry.getShortKey();
                return buf;
            }
        }
        
        // Still no buffer? Create one and store
        short nextId = (short) buffers.size();
        freeBuffer = new ChunkBuffer(chunksPerBuffer, extraAlloc);
        freeBufferId = nextId;
        buffers.put(nextId, freeBuffer);
        return freeBuffer;
    }
    
    public int addChunk(long addr, MaterialRegistry reg) {
        ChunkBuffer buf = freeBuffer;
        if (!buf.hasSpace()) {
            buf = findFreeBuffer();
        }
        
        // Put chunk to buffer
        int bufferId = buf.putChunk(addr);
        
        return freeBufferId << 16 | bufferId;
    }
    
}
