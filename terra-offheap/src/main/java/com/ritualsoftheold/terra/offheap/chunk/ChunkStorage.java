package com.ritualsoftheold.terra.offheap.chunk;

import java.util.function.Consumer;

import com.ritualsoftheold.terra.offheap.io.ChunkLoader;

import it.unimi.dsi.fastutil.shorts.Short2BooleanArrayMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanMap;
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
    
    private Short2BooleanMap loadedBuffers;
    
    /**
     * Chunk loader.
     */
    private ChunkLoader loader;
    
    /**
     * How many chunks there can be per buffer?
     */
    private int chunksPerBuffer;
    
    /**
     * Extra alloc for chunk buffers.
     */
    private int extraAlloc;
    
    public ChunkStorage(ChunkLoader loader, int chunksPerBuffer, int extraAlloc) {
        this.loader = loader;
        this.chunksPerBuffer = chunksPerBuffer;
        this.extraAlloc = extraAlloc;
        
        this.buffers = new Short2ObjectArrayMap<>();
        this.loadedBuffers = new Short2BooleanArrayMap();
    }
    
    public void requestBuffer(short bufferId, Consumer<ChunkBuffer> callback) {
        ChunkBuffer buf = buffers.get(bufferId);
        
    }
    
    public void save(short bufferId) {
        ChunkBuffer buf = buffers.get(bufferId);
        if (buf != null) {
            loader.saveChunks(bufferId, buf);
        }
    }
    
}
