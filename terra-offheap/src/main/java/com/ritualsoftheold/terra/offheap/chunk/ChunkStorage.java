package com.ritualsoftheold.terra.offheap.chunk;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.node.Chunk;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.io.ChunkLoader;
import com.ritualsoftheold.terra.offheap.memory.MemoryUseListener;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Chunk storage stores chunks in their memory representation.
 *
 */
public class ChunkStorage {
    
    private static final Memory mem = OS.memory();
    
    /**
     * Chunk buffers.
     */
    private Map<Short,ChunkBuffer> buffers;
    
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
    private Cache<Integer, OffheapChunk> chunkCache;
    
    /**
     * An id for buffer which currently has space.
     */
    private AtomicInteger freeBufferId;
    
    /**
     * Thread-safe deque which will supply free buffers
     * for new chunks which are allocated.
     */
    private Deque<ChunkBuffer> freeBuffers;
    
    private MemoryUseListener memListener;
    
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
        
        this.chunkCache = Caffeine.newBuilder()
                .maximumWeight(10000000)
                .weigher(new ChunkWeighter())
                .removalListener(new UnloadingRemovalListener())
                .build();
        this.freeBufferId = new AtomicInteger(loader.countBuffers()); // Free index=count of buffers (0-based)
        this.freeBuffers = new ConcurrentLinkedDeque<>();
        this.buffers = new ConcurrentHashMap<>(); // TODO need primitive concurrent map, can't find any library for it
    }
    
    public void setMemListener(MemoryUseListener listener) {
        this.memListener = listener;
    }
    
    /**
     * Gets a chunk buffer with given id. If it is not loaded or does not exist,
     * it will be loaded or created and this method might block caller thread.
     * @param bufferId Buffer id.
     * @return Chunk buffer object.
     */
    public ChunkBuffer getBuffer(short bufferId) {
        ChunkBuffer buf = buffers.get(bufferId);
        if (buf == null) { // Oops we need to load this
            buf = new ChunkBuffer(chunksPerBuffer, extraAlloc, (short) freeBufferId.getAndIncrement(), memListener); // Create buffer
            loader.loadChunks(bufferId, buf);
            buffers.put(bufferId, buf);
        }
        
        // We "needed" this buffer right now
        buf.setLastNeeded(System.currentTimeMillis());
        
        return buf;
    }
    
    /**
     * Directly retrieves chunk buffer that is certainly loaded.
     * If it is not loaded, bad things will happen.
     * @param bufferId Buffer id.
     * @return Chunk buffer object - hopefully.
     */
    public ChunkBuffer getBufferUnsafe(short bufferId) {
        return buffers.get(bufferId);
    }
    
    public CompletableFuture<ChunkBuffer> saveBuffer(short bufferId) {
        ChunkBuffer buf = buffers.get(bufferId);
        if (buf != null) {
            return CompletableFuture.supplyAsync(() -> loader.saveChunks(bufferId, buf), loaderExecutor);
        } else {
            throw new IllegalStateException("chunk buffer not even loaded!");
        }
    }
    
    /**
     * Gets a chunk with given id (consists of buffer and chunk id) and
     * creates "safe" wrapper for it. Note that this method might block
     * if buffer containing the chunk is not loaded.
     * @param chunkId Chunk id, which consists of buffer id and id inside
     * that buffer.
     * @param registry To resolve materials, if one does not use world ids.
     * @return Chunk wrapper.
     */
    public Chunk getChunk(int chunkId, MaterialRegistry registry) {
        OffheapChunk chunk = chunkCache.getIfPresent(chunkId);
        if (chunk == null) { // Not in cache...
            // TODO optimize memory allocations here
            // We have static-sized cache, why not recycle memory? (answer: thread safety, but still...)
            short bufferId = (short) (chunkId >>> 16);
            int index = chunkId & 0xffff;
            ChunkBuffer buf = getBuffer(bufferId);
            long addr = mem.allocate(DataConstants.CHUNK_UNCOMPRESSED);
            
            buf.unpack(index, addr); // Unpack data
            chunk = new OffheapChunk(registry);
            chunk.memoryAddress(addr); // Set memory address to point to data
            chunkCache.put(chunkId, chunk); // Cache what we just loaded
        }
        return chunk;
    }
    
    /**
     * Finds or creates a buffer which has space. Note that this does NOT
     * put it to deque of buffers; usually addChunk does it automatically.
     * @return A buffer which has space for at least one chunk.
     */
    private ChunkBuffer findFreeBuffer() {
        for (Map.Entry<Short, ChunkBuffer> entry : buffers.entrySet()) {
            ChunkBuffer buf = entry.getValue();
            if (buf.hasSpace()) {
                return buf;
            }
        }
        
        // Hmm, loaded buffers do not have space
        for (short i = 0; i < freeBufferId.get(); i++) {
            if (!buffers.containsKey(i)) { // We didn't process this yet
                ChunkBuffer buf = getBuffer(i);
                if (buf.hasSpace()) {
                    return buf;
                }
            }
        }
        
        // Still no buffer? Create one and store
        short nextId = (short) freeBufferId.getAndIncrement();
        ChunkBuffer freeBuf = new ChunkBuffer(chunksPerBuffer, extraAlloc, nextId, memListener);
        buffers.put(nextId, freeBuf);
        return freeBuf;
    }
    
    public int addChunk(long addr, MaterialRegistry reg) {
        ChunkBuffer buf;
        if (!freeBuffers.isEmpty()) {
            buf = freeBuffers.pollFirst(); // Take first element
            if (buf == null) { // Thread safety
                buf = findFreeBuffer();
            }
        } else {
            buf = findFreeBuffer();
        }
        
        // Mark that we needed the buffer
        buf.setLastNeeded(System.currentTimeMillis());
        
        // Put chunk to buffer
        int bufferId = buf.putChunk(addr);
        if (buf.hasSpace()) { // Resubmit this buffer to start
            freeBuffers.addFirst(buf);
        }
        
        return buf.getId() << 16 | bufferId;
    }
    
    /**
     * Ensures that a chunk with given id is loaded when this method returns.
     * @param id Full chunk id.
     */
    public long ensureLoaded(int id) {
        ChunkBuffer buf = getBuffer((short) (id >>> 16));
        System.err.println("buf: " + buf.getId());
        return buf.getChunkAddress(id & 0xffff);
    }
    
    /**
     * Gets all available chunk buffers at the moment it was called.
     * @return Copy of available chunk buffers collection.
     */
    public Collection<ChunkBuffer> getAllBuffers() {
        return new HashSet<>(buffers.values());
    }
    
    /**
     * Unloads buffer given as parameter. Make sure it is not in use!
     * @param id Buffer id.
     */
    public void unloadBuffer(short id) {
        ChunkBuffer buf = buffers.get(id);
        buf.unloadAll();
        buffers.remove(id);
    }
    
}
