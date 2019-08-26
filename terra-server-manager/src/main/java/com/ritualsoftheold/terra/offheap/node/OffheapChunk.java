package com.ritualsoftheold.terra.offheap.node;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.chunk.TooManyMaterialsException;
import com.ritualsoftheold.terra.offheap.chunk.compress.ChunkFormat;
import com.ritualsoftheold.terra.offheap.chunk.compress.EmptyChunkFormat;
import com.ritualsoftheold.terra.offheap.material.Registry;
import com.ritualsoftheold.terra.offheap.MemoryArea;
import com.ritualsoftheold.terra.offheap.Pointer;
import com.ritualsoftheold.terra.offheap.data.BufferWithFormat;
import com.ritualsoftheold.terra.offheap.data.OffheapNode;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;
import xerial.larray.LByteArray;

/**
 * A chunk that stores its blocks outside of JVM heap.
 *
 */
public class OffheapChunk implements Chunk, OffheapNode {

    private static final Memory mem = OS.memory();
    
    /**
     * Index of this chunk in the chunk buffer.
     */
    private final int index;
    
    /**
     * Chunk buffer that contains this chunk.
     */
    private final ChunkBuffer buffer;
    
    public static class Storage extends MemoryArea implements AutoCloseable {
        
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
        
        private volatile int userCount;
        
        public Storage(ChunkFormat format, @Pointer long addr, long length) {
            super(addr, length, null, false);
            this.format = format;
        }

        public Storage(ChunkFormat format, MemoryArea area) {
            super(area.memoryAddress(), area.length(), null, false);
            this.format = format;
        }

        @Override
        public void close() {
            userCountHandle.getAndAdd(this, -1);
        }
        
        void open() {
            userCountHandle.getAndAdd(this, 1);
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
     * 
     *  @see TooManyMaterialsException The exception we may catch when
     *  applying changes.
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
         * @param query Query to add.
         */
        public void addQuery(long query) {
            long curAddr;
            int i;
            while (true) { // Acquire a slot
                curAddr = addr;
                i = index.getAndIncrement();
                if (i >= size) { // No space available
                    // Attempt to swap queues
                    requestFlush(false);
                } else { // Got space
                    break;
                }
            }
            
            // Write query to its slot, which is guaranteed to be valid now
            mem.writeVolatileLong(curAddr + i * 8, query);
            writtenIndex.incrementAndGet(); // TODO reduce amount of atomic operations here
        }
        
        public void forceFlush() {
            index.getAndIncrement();
            requestFlush(true);
        }
        
        private void requestFlush(boolean force) {
            while (true) {
                Thread.onSpinWait();
                if (!force && index.get() < size) {
                    return; // Someone managed to flush
                }
                if (canSwap.compareAndSet(true, false)) {
                    break; // We're going to flush!
                }
            }
            
            // Wait for any volatile writes to queue
            while (writtenIndex.get() != index.get() - 1) {
                Thread.onSpinWait();
            }
            int howMany = writtenIndex.get();
            
            swapQueues();
            doFlush(howMany);
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
        
        private void doFlush(int howMany) {
            Storage result = applyQueries(chunk.storage, new ChangeIterator(swapAddr, howMany));
            if (result != null) { // Put new storage there if needed
                chunk.storage = result;
                chunk.buffer.getAllocator().free(result.memoryAddress(), result.length());
            }

            // Signal that swapAddr could be now swapped in place of addr
            canSwap.set(true);
        }
        
        private Storage applyQueries(Storage storage, ChangeIterator iterator) {
            Storage result = storage.format.processQueries(chunk, storage, iterator); // Process -> new storage
            
            // Apply changes recursively until all of them have been applied
            if (iterator.hasNext()) {
                Storage ret = applyQueries(result, iterator);
                chunk.buffer.getAllocator().free(result.memoryAddress(), result.length());
                return ret;
            }
            
            return result;
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
            // TODO investigate if opaque reads could be done with Unsafe
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
     */
    private ConcurrentMap<Integer,Object> refs;

    private float x;
    private float y;
    private float z;

    private LByteArray lByteArray;
    
    public OffheapChunk(int index, ChunkBuffer buffer, long queueAddr, long swapAddr, int queueSize) {
        // Permanent (final) chunk parameters
        this.index = index;
        this.buffer = buffer;
        this.queue = new ChangeQueue(this, queueAddr, swapAddr, queueSize);
        this.refs = new ConcurrentHashMap<>();

        // Initially empty chunk, will change later
        this.storage = new Storage(EmptyChunkFormat.INSTANCE, 0, 0);
    }

    public void setlByteArray(LByteArray lByteArray) {
        this.lByteArray = lByteArray;
    }

    public void setCoordinates(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    @Override
    public Type getNodeType() {
        return Type.CHUNK;
    }

    @Override
    public BufferWithFormat getBuffer() {
        /*
         * Buffers created by OffheapChunk represent the latest data in reads,
         * excluding yet-to-be-applied change queries, IF and ONLY IF the chunk
         * format has not changed. This conforms with the API Javadoc
         * of this method.
         */
        Storage storage = getStorage(); // Adds 1 to user count
        return storage.format.createBuffer(this, storage);
    }

    public LByteArray getLArray(){
        return lByteArray;
    }

    @Override
    public long memoryAddress() {
        return storage.memoryAddress();
    }

    @Override
    public int memoryLength() {
        return (int) storage.length(); // Assume that chunk length is never over 2gb
    }

    public Storage getStorage() {
        Storage storage = this.storage; // Acquire from field so it won't change
        storage.open();
        return storage;
    }

    public Storage getStorageInternal() {
        return storage;
    }
    
    public void setStorageInternal(Storage storage) {
        this.storage = storage;
    }
    
    public void queueChange(long entry) {
        queue.addQuery(entry);
    }
    
    /**
     * Queues a block change to this chunk. Block with given index will soon
     * have its id changed to given value. The change will not be reflected
     * immediately, but all queued changes will be applied in order.
     * @param index Index of block to change.
     * @param blockId Block id to apply.
     */
    public void queueChange(int index, int blockId) {
        queueChange(index << 24 | blockId);
    }
    
    /**
     * Immediately applies all pending block changes using the current thread,
     * which will be blocked for a short amount of time.
     */
    public void flushChanges() {
        queue.forceFlush();
    }
    
    /**
     * For internal use only. Acquires extra data references for blocks of this
     * chunk.
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
    
    public Registry getWorldMaterialRegistry() {
        return buffer.getStorage().getRegistry();
    }
    
    /**
     * Gets index of this chunk inside its chunk buffer.
     * @return Chunk index.
     */
    public int getIndex() {
        return index;
    }
    
    /**
     * Gets full chunk id that includes both chunk buffer id and index
     * of this chunk inside it.
     * @return Full chunk id.
     */
    public int getFullId() {
        return buffer.getId() << 16 | index;
    }
    
    /**
     * Gets the chunk buffer containing this chunk.
     * @return Chunk buffer.
     */
    public ChunkBuffer getChunkBuffer() {
        return buffer;
    }
}
