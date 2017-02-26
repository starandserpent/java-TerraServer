package com.ritualsoftheold.terra.offheap.chunk;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ritualsoftheold.terra.node.Chunk;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Stores chunk data in offheap memory.
 *
 */
public class ChunkStorage {
    
    private static Memory mem = OS.memory();
    
    /**
     * Map of offheap chunks.
     */
    private Map<OffheapChunk,ChunkBuffer> chunks;
    
    private Set<ChunkBuffer> buffers;
    
    private ChunkBuffer currentBuffer;
    
    private int bufferLength;
   
    private int chunksPerBuffer;
    
    private OffheapWorld world;
    
    public ChunkStorage() {
        chunks = new HashMap<>();
        buffers = new HashSet<>();
    }
    
    private ChunkBuffer currentBuffer(int length) {
        if (currentBuffer.hasSpace(length)) {
            return currentBuffer;
        }
        
        // Not enough space? Take next buffer
        for (ChunkBuffer buffer : buffers) {
            if (buffer.hasSpace(length)) {
                currentBuffer = buffer;
                return buffer;
            }
        }
        
        // No buffer has enough scape? Create one
        // TODO before this, we should perform chunk unloading+cleanup
        ChunkBuffer buffer = new ChunkBuffer(bufferLength, chunksPerBuffer);
        currentBuffer = buffer;
        return buffer;
    }
    
    public void loadChunk(long addr, int length) {
        ChunkBuffer buf = currentBuffer(length); // Buffer where we can put the chunk
        
        OffheapChunk chunk = new OffheapChunk(world, length); // New offheap 
        long dataAddr = buf.addChunk(chunk); // chunk -> buffer
        chunks.put(chunk, buf); // Store chunk obj (not actual block data!) and buffer for it
        mem.copyMemory(addr, dataAddr, length); // Copy actual data to chunk
    }
    
    public void updateChunk(OffheapChunk chunk, int length) {
        ChunkBuffer buf = chunks.get(chunk); // Get correct buffer
        buf.updateChunk(chunk.bufferId(), length); // Forward update request to it
    }
}
