package com.ritualsoftheold.terra.offheap.node;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicInteger;

import com.ritualsoftheold.terra.buffer.BlockBuffer;
import com.ritualsoftheold.terra.buffer.TerraRef;
import com.ritualsoftheold.terra.node.Chunk;
import com.ritualsoftheold.terra.offheap.Pointer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.chunk.compress.ChunkFormat;
import com.ritualsoftheold.terra.offheap.data.OffheapNode;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

public class OffheapChunk implements Chunk, OffheapNode {

    private static final Memory mem = OS.memory();
    
    /**
     * Chunk buffer that contains this chunk.
     */
    private final ChunkBuffer buffer;
    
    public static class Storage implements AutoCloseable {
        
        private static final VarHandle userCountHandle;
        
        static {
            try {
                userCountHandle = MethodHandles
                    .privateLookupIn(Storage.class, MethodHandles.lookup())
                    .findVarHandle(Storage.class, "userCount", int.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new AssertionError("failed to get var handle");
            }
        }
        
        /**
         * Data format that this chunk uses.
         */
        public final ChunkFormat format;
        
        /**
         * Memory address of data.
         */
        public final @Pointer long address;
        
        /**
         * Length of data at the address.
         */
        public final int length;
        
        private volatile int userCount;
        
        public Storage(ChunkFormat format, @Pointer long addr, int length) {
            this.format = format;
            this.address = addr;
            this.length = length;
        }

        @Override
        public void close() throws Exception {
            userCountHandle.getAndAdd(this, -1);
        }
        
        void open() {
            userCountHandle.getAndAdd(this, -1);
        }
    }
    
    private volatile Storage storage;
    
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
            Storage storage = chunk.storage; // Take old storage
            Storage result = storage.format.processQueries(chunk, addr, size); // Process _> new storage
            
            // Swap new storage in place of the old one
            chunk.storage = result; // Field is volatile so this is safe
            
            // Give old storage back to chunk buffer for future use
            // TODO implement this
        }
        
        private void cleanup() {
            index.set(0);
        }
    }
    
    /**
     * Queue for changes to be applied to this chunk.
     */
    private final ChangeQueue queue;
    
    public OffheapChunk(ChunkBuffer buffer, long queueAddr, int queueSize) {
        this.buffer = buffer;
        this.queue = new ChangeQueue(this, queueAddr, queueSize);
    }

    @Override
    public Type getNodeType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BlockBuffer getBuffer() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long memoryAddress() {
        return storage.address;
    }

    @Override
    public int memoryLength() {
        return storage.length;
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

    public Storage getStorage() {
        Storage storage = this.storage; // Acquire from field so it won't change
        storage.open();
        return storage;
    }
}
