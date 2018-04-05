package com.ritualsoftheold.terra.offheap.node;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.ritualsoftheold.terra.buffer.BlockBuffer;
import com.ritualsoftheold.terra.buffer.TerraRef;
import com.ritualsoftheold.terra.node.Chunk;
import com.ritualsoftheold.terra.offheap.Pointer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.chunk.compress.ChunkFormat;
import com.ritualsoftheold.terra.offheap.chunk.compress.ChunkFormat.ProcessResult;
import com.ritualsoftheold.terra.offheap.data.OffheapNode;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

public class OffheapChunk implements Chunk, OffheapNode {

    private static final Memory mem = OS.memory();
    
    /**
     * Chunk buffer that contains this chunk.
     */
    private final ChunkBuffer buffer;
    
    /**
     * Data format that this chunk uses.
     */
    private final ChunkFormat format;
    
    /**
     * Memory address of data.
     */
    private final @Pointer long addr;
    
    /**
     * Length of data at the address.
     */
    private final int length;
    
    /**
     * Change queue for this chunk.
     *
     */
    public static class ChangeQueue {
        
        /**
         * Memory address of queue data.
         */
        private final @Pointer long addr;
        
        /**
         * First free index in the queue.
         */
        private final AtomicInteger index;
        
        /**
         * Size of this change queue.
         */
        private final int size;
        
        private final OffheapChunk chunk;
        
        public ChangeQueue(OffheapChunk chunk, @Pointer long addr, int size) {
            this.addr = addr;
            this.index = new AtomicInteger(0);
            this.size = size;
            this.chunk = chunk;
        }
        
        /**
         * Adds a change query to this queue. If there is no space, queue
         * will be flushed and the call will block for that amount of time.
         * @param query
         */
        public void addQuery(long query) {
            int i;
            while (true) { // Acquire a slot
                i = index.getAndIncrement();
                if (i >= size) { // No space available
                    // Flush immediately
                    requestFlush();
                } else { // Got space
                    break;
                }
            }
            
            // Write query to its slot, which is quaranteed to be valid now
            mem.writeVolatileLong(addr + i * 8, query);
        }
        
        private synchronized void requestFlush() {
            if (index.get() < size) { // Someone else flushed before us
                return; // No action needed
            }
            
            // If we're the first one, do actual flushing
            doFlush();
            cleanup();
        }
        
        private void doFlush() {
            
            ProcessResult result = chunk.format.processQueries(chunk, addr, size);
            
        }
        
        private void cleanup() {
            index.set(0);
        }
    }
    
    /**
     * Queue for changes to be applied to this chunk.
     */
    private final ChangeQueue queue;
    
    public OffheapChunk(ChunkBuffer buffer, ChunkFormat format, long addr, int length, long queueAddr, int queueSize) {
        this.buffer = buffer;
        this.format = format;
        this.addr = addr;
        this.length = length;
        this.queue = new ChangeQueue(this, queueAddr, queueSize);
    }

    @Override
    public Type getNodeType() {
        return Type.CHUNK;
    }

    @Override
    public BlockBuffer getBuffer() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long memoryAddress() {
        return addr;
    }

    @Override
    public int memoryLength() {
        return length;
    }

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub
        
    }

    @Override
    public TerraRef createStaticRef(int size) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TerraRef createDynamicRef(int initialSize) {
        // TODO Auto-generated method stub
        return null;
    }

}
