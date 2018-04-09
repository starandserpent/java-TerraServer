package com.ritualsoftheold.terra.offheap.node;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.ritualsoftheold.terra.buffer.BlockBuffer;
import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.node.Chunk;
import com.ritualsoftheold.terra.offheap.Pointer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.chunk.compress.ChunkFormat;
import com.ritualsoftheold.terra.offheap.chunk.compress.EmptyChunkFormat;
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
        public void close() {
            userCountHandle.getAndAdd(this, -1);
        }
        
        void open() {
            userCountHandle.getAndAdd(this, -1);
        }
        
        public int getUserCount() {
            return userCount;
        }
    }
    
    private volatile Storage storage;
    
    /**
     * Implements a queue with minimal amount of blocking that I could
     * somewhat make work.
     * 
     * During normal usage (there is space in queue), no blocking is done.
     * When the queue is full, however, it must be flushed.
     * That causes some blocking:
     * 
     * 1) Queue is completely blocked for a very short amount of time,
     * before it is swapped to another. VERY SHORT amount of time!
     * 2) One calling threads is blocked during the flush operation.
     * Even that shouldn't take very long time.
     * 
     * But there is a worse scenario, if queue size is miscofigured:
     * 3) Queue is completely blocked, waiting for swap queue to be
     * completely flushed. This might cause significant slowdowns!
     */
    public static class ChangeQueue {
        
        /**
         * Initial memory address of queue.
         */
        private volatile @Pointer long addr;
        
        /**
         * Memory address will be swapped with this.
         */
        private volatile @Pointer long swapAddr;
        
        /**
         * First free index in the queue.
         */
        private final AtomicInteger index;
        
        private final AtomicInteger writtenIndex;
        
        /**
         * Size of this change queue.
         */
        private final int size;
        
        private final OffheapChunk chunk;
        
        /**
         * If swapping buffers right now would be safe.
         */
        private final AtomicBoolean canSwap;
        
        public ChangeQueue(OffheapChunk chunk, @Pointer long addr, @Pointer long swapAddr, int size) {
            this.addr = addr;
            this.swapAddr = swapAddr;
            this.index = new AtomicInteger(0);
            this.writtenIndex = new AtomicInteger(0);
            this.size = size;
            this.chunk = chunk;
            this.canSwap = new AtomicBoolean(true);
        }
        
        /**
         * Adds a change query to this queue. If there is no space, queue
         * will be flushed and the call will block for that amount of time.
         * @param query
         */
        public void addQuery(long query) {
            long curAddr;
            int i;
            while (true) { // Acquire a slot
                curAddr = addr;
                i = index.getAndIncrement();
                if (i >= size) { // No space available
                    // Attempt to swap queues
                    requestFlush();
                } else { // Got space
                    break;
                }
            }
            
            // Write query to its slot, which is guaranteed to be valid now
            mem.writeVolatileLong(curAddr + i * 8, query);
            writtenIndex.incrementAndGet();
        }
        
        private void requestFlush() {
            while (true) {
                Thread.onSpinWait();
                if (index.get() < size) {
                    return; // Someone managed to flush
                }
                if (canSwap.compareAndSet(true, false)) {
                    break; // We're going to flush!
                }
            }
            
            // Wait for any volatile writes to queue
            while (writtenIndex.get() != index.get()) {
                Thread.onSpinWait();
            }
            
            swapQueues();
            doFlush();
        }
        
        private void swapQueues() {            
            // Swap addr and swapAddr
            long processAddr = addr;
            addr = swapAddr;
            swapAddr = processAddr;
            
            // Set index to 0, because queue space is now available
            index.set(0);
            writtenIndex.set(0);
        }
        
        private void doFlush() {
            Storage storage = chunk.storage; // Take old storage
            Storage result = storage.format.processQueries(chunk, new ChangeIterator(swapAddr, size)); // Process -> new storage
            
            // Swap new storage in place of the old one if needed
            if (result != null) {
                chunk.storage = result; // Field is volatile so this is safe
            }
            
            // Give old storage back to chunk buffer for future use
            // TODO implement this
            
            // Signal that swapAddr could be now swapped in place of addr
            canSwap.set(true);
        }
    }
    
    public static class ChangeIterator {
        
        private final @Pointer long queue;
        
        private final int size;
        
        private int index;
        
        private long entry;
        
        public ChangeIterator(long queue, int size) {
            this.queue = queue;
            this.size = 8 * size;
        }
        
        public boolean hasNext() {
            return index < size;
        }
        
        public void next() {
            // TODO potentially just use readLong, it should work on x86
            entry = mem.readVolatileLong(queue + index);
            index += 8;
        }
        
        public void ignoreNext() {
            index -= 8;
        }
        
        public int getIndex() {
            return (int) (entry >>> 24 & 0x3ffff);
        }
        
        public int getBlockId() {
            return (int) (entry & 0xffffff);
        }
        
        // When other query types are added, add accessors to their data here
    }
    
    /**
     * Queue for changes to be applied to this chunk.
     */
    private final ChangeQueue queue;
    
    /**
     * Contains references that blocks have. Key is block id, value is the ref.
     * TODO implement this in smarter way, ConcurrentHashMap doesn't work
     * ideally with primitive keys.
     */
    private ConcurrentMap<Integer,Object> refs;
    
    private MaterialRegistry materialRegistry;
    
    public OffheapChunk(ChunkBuffer buffer, long queueAddr, long swapAddr, int queueSize) {
        // Permanent (final) chunk parameters
        this.buffer = buffer;
        this.queue = new ChangeQueue(this, queueAddr, swapAddr, queueSize);
        this.refs = new ConcurrentHashMap<>();
        
        // Initially empty chunk, will change later
        this.storage = new Storage(EmptyChunkFormat.INSTANCE, 0, 0);
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
        return storage.address;
    }

    @Override
    public int memoryLength() {
        return storage.length;
    }

    public Storage getStorage() {
        Storage storage = this.storage; // Acquire from field so it won't change
        storage.open();
        return storage;
    }
    
    public void queueChange(long entry) {
        queue.addQuery(entry);
    }
    
    public void queueChange(int index, int blockId) {
        queueChange(index << 24 | blockId);
    }
    
    /**
     * For internal use only.
     * @return Ref map as it is.
     */
    public ConcurrentMap<Integer,Object> getRefMap() {
        return refs;
    }

    @Override
    public Object getRef(int blockId) {
        return refs.get(blockId);
    }

    @Override
    public void setRef(int blockId, Object ref) {
        refs.put(blockId, ref);
    }
    
    public MaterialRegistry getWorldMaterialRegistry() {
        return materialRegistry;
    }

    public ChunkBuffer getChunkBuffer() {
        return buffer;
    }
}
