package com.ritualsoftheold.terra.offheap.chunk;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

import com.ritualsoftheold.terra.offheap.chunk.ChunkLoaderThread.BufferEntry;
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
    
    /**
     * Loader threads, so they are not garbage collected.
     */
    private ChunkLoaderThread[] loaderThreads;
    
    /**
     * Loader queue, accessed by {@link #loaderThreads}.
     */
    private BlockingQueue<BufferEntry> loaderQueue;
    
    /**
     * Cache of chunks, which are NOT compressed.
     */
    private Int2ObjectMap<OffheapChunk> chunkCache;
    
    /**
     * Initializes new chunk storage. Usually this should be done once per world.
     * @param loader Chunk loader, responsible for loading and optionally saving chunks.
     * @param threadCount How many threads to start for asynchronous chunk
     * loading and saving? They all will call same chunk loader, so make sure
     * the operations in it are thread safe.
     * @param chunksPerBuffer Maximum amount of chunks per chunk buffer.
     * @param extraAlloc How many bytes to allocate at end of each chunk
     * in all chunk buffers.
     */
    public ChunkStorage(ChunkLoader loader, int threadCount, int chunksPerBuffer, int extraAlloc) {
        initLoaderThreads(loader, threadCount);
        loaderQueue = new ArrayBlockingQueue<>(2000); // Should never ever run out of space
        
        this.chunksPerBuffer = chunksPerBuffer;
        this.extraAlloc = extraAlloc;
        
        chunkCache = new Int2ObjectOpenHashMap<>();
        
        this.buffers = new Short2ObjectArrayMap<>();
    }
    
    private void initLoaderThreads(ChunkLoader loader, int threadCount) {
        loaderThreads = new ChunkLoaderThread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            loaderThreads[i] = new ChunkLoaderThread(loaderQueue, loader);
        }
    }
    
    public void requestBuffer(short bufferId, Consumer<ChunkBuffer> callback) {
        ChunkBuffer buf = buffers.get(bufferId);
        if (buf == null) { // Oops we need to load this
            buf = new ChunkBuffer(chunksPerBuffer, extraAlloc); // Create buffer
            BufferEntry entry = new BufferEntry(false, bufferId, buf, (buffer -> {
                buffers.put(bufferId, buffer); // Put to map
                callback.accept(buffer);
            })); // Create entry for queue
            loaderQueue.add(entry); // Add to queue
        } else {
            callback.accept(buf);
        }
    }
    
    public void saveBuffer(short bufferId, Consumer<ChunkBuffer> callback) {
        ChunkBuffer buf = buffers.get(bufferId);
        if (buf != null) {
            BufferEntry entry = new BufferEntry(true, bufferId, buf, callback); // Create entry for queue
            loaderQueue.add(entry); // Put to queue
        }
    }
    
    public void requestChunk(int chunkId, Consumer<OffheapChunk> callback) {
        OffheapChunk chunk = chunkCache.get(chunkId);
        if (chunk == null) { // Not in cache...
            short bufferId = (short) (chunkId >>> 16);
            requestBuffer(bufferId, (buf -> {
                // TODO do this once chunk rework with Snappy is done
            }));
        }
    }
    
}
