package com.ritualsoftheold.terra.offheap.node;

import java.util.concurrent.atomic.AtomicInteger;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.material.TerraMaterial;
import com.ritualsoftheold.terra.node.Chunk;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.Pointer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.chunk.compress.ChunkFormat;
import com.ritualsoftheold.terra.offheap.chunk.iterator.ChunkIterator;
import com.ritualsoftheold.terra.offheap.data.OffheapNode;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

public class OffheapChunk implements Chunk, OffheapNode {

    private static final Memory mem = OS.memory();
    
    /**
     * Chunk buffer that contains this chunk.
     */
    private ChunkBuffer buffer;
    
    /**
     * Data format that this chunk uses.
     */
    private volatile ChunkFormat format;
    
    /**
     * Memory address of data.
     */
    private volatile @Pointer long addr;
    
    /**
     * Length of data at the address.
     */
    private volatile int length;
    
    /**
     * How much memory is allocated for the data. This might be
     * quite a lot more than length of data in some cases.
     */
    private volatile int allocated;
    
    /**
     * Change queue for this chunk.
     *
     */
    public static class ChangeQueue {
        
        /**
         * Memory address of queue data.
         */
        private volatile @Pointer long addr;
        
        /**
         * First free index in the queue.
         */
        private AtomicInteger index;
        
        private volatile long size;
        
        public void addQuery(long query) {
            int i = index.getAndIncrement();
            
            mem.writeVolatileLong(addr + i * 8, i);
        }
    }

}
