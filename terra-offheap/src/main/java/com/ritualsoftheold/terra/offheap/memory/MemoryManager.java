package com.ritualsoftheold.terra.offheap.memory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.StampedLock;

import com.ritualsoftheold.terra.offheap.octree.OctreeStorage;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkStorage;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;

/**
 * Manages offheap memory.
 *
 */
public class MemoryManager implements MemoryUseListener {
        
    private volatile OffheapWorld world;
    private volatile OctreeStorage octreeStorage;
    private volatile ChunkStorage chunkStorage;
    
    /**
     * Preferred memory size. As long as it is below this, no need to act.
     */
    private final long preferredSize;
    
    /**
     * Maximum memory usage. If this is exceeded, server will enter panic mode
     * and potentially crash.
     */
    private final long maxSize;
    
    /**
     * Currently used offheap memory.
     */
    private final AtomicLong usedSize;
    
    /**
     * User specified memory panic handler.
     */
    private final MemoryPanicHandler userPanicHandler;
    
    /**
     * The thread which is used for memory manager actions.
     */
    private final Thread managerThread;
    
    /**
     * Controls when manager thread runs.
     */
    private volatile CountDownLatch managerLatch;
    
    /**
     * Load markers acquire read for this lock, we acquire write for it.
     */
    private final StampedLock loadMarkerLock;
    
    private class ManagerThread extends Thread {
        
        @Override
        public void run() {
            try {
                managerLatch.await();
            } catch (InterruptedException e) {
                // If waiting is interrupted, we should just continue NOW
                // At least it is better than quitting and getting OS OOM kill us later
            }
            System.out.println("Begin unload, usedSize: " + usedSize);
            long usedSizeVal = usedSize.get();
            if (usedSizeVal > maxSize) { // Too much memory used already, free or panic!
                unload(usedSizeVal - preferredSize, criticalPanicHandler);
            } else if (usedSizeVal > preferredSize) { // Used memory is ok, but less would be better
                unload(usedSizeVal - preferredSize, userPanicHandler);
            }
            
            // Replace the old latch
            managerLatch = new CountDownLatch(1);
        }
    }
    
    /**
     * Controls what happens when unloading does not fully succeed and used
     * memory before it exceeded maximum allowed amount.
     */
    private CriticalPanicHandler criticalPanicHandler;
    
    private class CriticalPanicHandler implements MemoryPanicHandler {

        @Override
        public PanicResult goalNotMet(long goal, long possible) {
            if (usedSize.get() - possible < maxSize) {
                // We can't meet goal, but won't still get out of memory
                return userPanicHandler.goalNotMet(goal, possible);
            } else {
                // Ouch, we're out of memory even after this unload
                return userPanicHandler.outOfMemory(maxSize, usedSize.get(), possible);
            }
        }

        @Override
        public PanicResult outOfMemory(long max, long used, long possible) {
            throw new UnsupportedOperationException("misuse of critical panic handler");
        }
        
    }
    
    /**
     * Initializes new instance of the memory manager.
     * @param world World which will use this.
     * @param preferred
     * @param max
     * @param panicHandler
     */
    public MemoryManager(OffheapWorld world, long preferred, long max, MemoryPanicHandler panicHandler) {
        this.world = world;
        this.preferredSize = preferred;
        this.maxSize = max;
        this.usedSize = new AtomicLong();
        this.userPanicHandler = panicHandler;
        
        this.managerLatch = new CountDownLatch(1);
        this.managerThread = new ManagerThread();
        this.managerThread.start();
        this.criticalPanicHandler = new CriticalPanicHandler();
        this.loadMarkerLock = new StampedLock();
    }
    
    public void initialize(OctreeStorage octreeStorage, ChunkStorage chunkStorage) {
        this.octreeStorage = octreeStorage;
        this.chunkStorage = chunkStorage;
    }
    
    /**
     * Queue unloading of unused resources to happen as soon as possible.
     * If it is currently in progress, nothing will happen.
     */
    public void queueUnload() {
        System.out.println("Queue unload");
        managerLatch.countDown();
    }
    
    
    /**
     * Attempts to unload chunks and octrees until specific amount of memory
     * has been freed.
     * @param goal How much we need memory.
     */
    public void unload(long goal, MemoryPanicHandler panicHandler) {
        // Track how much we'd actually free memory
        long freed = 0;
        
        // Unload (and save) octrees
        // Begin at 1, master group must not be unloaded; ever
        for (int i = 1; i < octreeStorage.getGroupCount(); i++) {
            if (octreeStorage.getUsedCount(i) < 1) { // Need to unload this group
                System.out.println("Can unload: " + i);
                octreeStorage.removeOctrees(i, true); // Remove groups, but save first
                
                freed += world.getOctreeStorage().getGroupSize();
                if (freed >= goal) { // Hey, we can now release enough
                    break;
                }
            }
        }
        System.out.println("Octrees to free: " + freed);
        
        // Mark which chunks to unload
        AtomicReferenceArray<ChunkBuffer> allBuffers = world.getChunkStorage().getAllBuffers();
        if (freed < goal) { // Only do this if unloading octrees wouldn't save enough space
            for (int i = 0; i < allBuffers.length(); i++) {
                ChunkBuffer buf = allBuffers.get(i); // TODO performance
                if (buf == null) {
                    // Buffer is not loaded at the moment, skip it
                    continue;
                }
                
                if (chunkStorage.getUsedCount(i) < 1) { // If not used, mark for unloading
                    chunkStorage.unloadBuffer(buf.getId(), true);
                    
                    freed += buf.getMemorySize();
                    if (freed >= goal) { // Hey, we can now release enough
                        break;
                    }
                }
            }
        }
        
        System.out.println("Could free: " + freed);
        
        // Ok, everything saved and so on... Can we save enough?
        if (freed < goal) { // Nope, and that could be bad
            MemoryPanicHandler.PanicResult result = panicHandler.goalNotMet(goal, freed);
            if (result == MemoryPanicHandler.PanicResult.INTERRUPT) { // Stop here
                return;
            }
        }
    }
    
    // Manage usedSize from information that comes from octree/chunk storages
    
    @Override
    public void onAllocate(long amount) {
        usedSize.addAndGet(amount);
    }

    @Override
    public void onFree(long amount) {
        usedSize.addAndGet(-amount);
    }
    
    public StampedLock getLoadMarkerLock() {
        return loadMarkerLock;
    }
}
