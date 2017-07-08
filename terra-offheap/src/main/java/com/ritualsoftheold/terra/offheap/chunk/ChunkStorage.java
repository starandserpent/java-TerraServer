package com.ritualsoftheold.terra.offheap.chunk;

import java.util.Deque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.node.Chunk;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.io.ChunkLoader;
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
    private Map<Integer,OffheapChunk> chunkCache;
    
    /**
     * An id for buffer which currently has space.
     */
    private AtomicInteger freeBufferId;
    
    /**
     * Thread-safe deque which will supply free buffers
     * for new chunks which are allocated.
     */
    private Deque<ChunkBuffer> freeBuffers;
    
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
        
        this.chunkCache = new ConcurrentHashMap<>();
        this.freeBufferId = new AtomicInteger(loader.countBuffers()); // Free index=count of buffers (0-based)
        this.freeBuffers = new ConcurrentLinkedDeque<>();
        this.buffers = new ConcurrentHashMap<>(); // TODO need primitive concurrent map, can't find any library for it
    }
    
    public CompletableFuture<ChunkBuffer> requestBuffer(short bufferId) {
        ChunkBuffer buf = buffers.get(bufferId);
        if (buf == null) { // Oops we need to load this
            ChunkBuffer newBuf = new ChunkBuffer(chunksPerBuffer, extraAlloc, (short) freeBufferId.getAndIncrement()); // Create buffer
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
            buf = new ChunkBuffer(chunksPerBuffer, extraAlloc, (short) freeBufferId.getAndIncrement()); // Create buffer
            loader.loadChunks(bufferId, buf);
            buffers.put(bufferId, buf);
        }
        
        // We "needed" this buffer right now
        buf.setLastNeeded(System.currentTimeMillis());
        
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
            // We have static-sized cache, why not recycle memory? (answer: thread safety, but still...)
            short bufferId = (short) (chunkId >>> 16);
            int index = chunkId & 0xffff;
            CompletableFuture<ChunkBuffer> bufFuture = requestBuffer(bufferId);
            CompletableFuture<Chunk> chunkFuture = CompletableFuture.supplyAsync(() -> {
                long addr = mem.allocate(DataConstants.CHUNK_UNCOMPRESSED);
                ChunkBuffer buf = bufFuture.join(); // Wait for the other future
                
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
        ChunkBuffer freeBuf = new ChunkBuffer(chunksPerBuffer, extraAlloc, nextId);
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
        return buf.getChunkAddress(id & 0xffff);
    }
    
}
