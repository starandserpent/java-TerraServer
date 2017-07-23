package com.ritualsoftheold.terra.offheap.world;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.ritualsoftheold.terra.node.Chunk;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.world.MemoryPanicHandler.PanicResult;

import it.unimi.dsi.fastutil.bytes.ByteArraySet;
import it.unimi.dsi.fastutil.bytes.ByteSet;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Manages offheap memory.
 *
 */
public class MemoryManager {
    
    private static final Memory mem = OS.memory();
    
    private OffheapWorld world;
    
    /**
     * Preferred memory size. As long as it is below this, no need to act.
     */
    private long preferredSize;
    
    /**
     * Maximum memory usage. If this is exceeded, server will enter panic mode
     * and potentially crash.
     */
    private long maxSize;
    
    public MemoryManager(OffheapWorld world, long preferred, long max) {
        this.world = world;
        this.preferredSize = preferred;
        this.maxSize = max;
    }
    
    /**
     * Attempts to unload chunks and octrees until specific amount of memory
     * has been freed.
     * @param goal How much we need memory.
     */
    public void unload(long goal, MemoryPanicHandler panicHandler) {
        // Create sets which will contain addresses that are used
        LongSet usedOctreeGroups = new LongArraySet();
        Set<ChunkBuffer> usedChunkBufs = new ObjectOpenHashSet<>();
        
        // Populate sets with addresses
        world.updateLoadMarkers(new WorldLoadListener() {
            
            @Override
            public void octreeLoaded(long addr, long groupAddr, float x, float y, float z,
                    float scale) {
                usedOctreeGroups.add(groupAddr);
            }
            
            @Override
            public void chunkLoaded(long addr, ChunkBuffer buf, float x, float y, float z) {
                
            }
        }, true);
        
        // Track how much we'd actually free memory
        long freed = 0;
        
        // Mark which octrees to unload
        long groups = world.getOctreeStorage().getGroups();
        ByteSet unusedGroups = new ByteArraySet();
        Set<CompletableFuture<Long>> groupSavePending = new ObjectOpenHashSet<>();
        for (int i = 0; i < 256; i++) {
            long groupAddr = mem.readLong(groups + i * 8);
            if (!usedOctreeGroups.contains(groupAddr)) { // Need to unload this group
                unusedGroups.add((byte) i);
                groupSavePending.add(world.getOctreeStorage().saveGroup((byte) i));
                
                freed += world.getOctreeStorage().getGroupSize();
                if (freed >= goal) { // Hey, we can now release enough
                    break;
                }
            }
        }
        
        // Mark which chunks to unload
        Collection<ChunkBuffer> allBuffers = world.getChunkStorage().getAllBuffers();
        Set<ChunkBuffer> unusedBuffers = new ObjectOpenHashSet<>();
        Set<CompletableFuture<ChunkBuffer>> savePending = new ObjectOpenHashSet<>();
        if (freed < goal) { // Only do this if unloading octrees wouldn't save enough space
            for (ChunkBuffer buf : allBuffers) {
                if (!usedChunkBufs.contains(buf)) { // If not used, mark for unloading
                    unusedBuffers.add(buf);
                    savePending.add(world.getChunkStorage().saveBuffer(buf.getId()));
                    
                    freed += buf.calculateSize();
                    if (freed >= goal) { // Hey, we can now release enough
                        break;
                    }
                }
            }
        }
        
        // Finish any pending save operations BEFORE entering critical section
        for (CompletableFuture<Long> future : groupSavePending) {
            future.join();
        }
        for (CompletableFuture<ChunkBuffer> future : savePending) {
            future.join();
        }
        
        // Ok, everything saved and so on... Can we save enough?
        if (freed < goal) { // Nope, and that could be bad
            PanicResult result = panicHandler.goalNotMet(goal, freed);
            if (result == PanicResult.INTERRUPT) { // Stop here
                return;
            } else if (result == PanicResult.FREEZE) { // EVERYONE, STOP RIGHT NOW!
                doFreeze(panicHandler);
            }
        }
        
        // Perform the performance critical operation with exclusive access
        unloadCritical(goal, unusedGroups, unusedBuffers);
    }
    
    /**
     * Performs actual unloading in world exclusive access.
     * @param goal
     * @param unusedGroups
     * @param unusedBuffers
     */
    private void unloadCritical(long goal, ByteSet unusedGroups, Set<ChunkBuffer> unusedBuffers) {
        long stamp = world.enterExclusive();
        
        // Unload unused octree groups ("remove" them)
        for (byte index : unusedGroups) {
            world.getOctreeStorage().removeOctrees(index);
        }
        
        // Unload unused chunk buffers
        for (ChunkBuffer buf : unusedBuffers) {
            world.getChunkStorage().unloadBuffer(buf.getId());
        }
        
        world.leaveExclusive(stamp);
    }
    
    /**
     * Essentially "freezes" the world by acquiring exclusive access.
     * Panic handler will then be called, and optionally, exclusive
     * access automatically ended.
     * @param panicHandler Panic handler to call.
     */
    private void doFreeze(MemoryPanicHandler panicHandler) {
        long stamp = world.enterExclusive();
        boolean shouldLeave = panicHandler.handleFreeze(stamp);
        if (shouldLeave) {
            world.leaveExclusive(stamp);
        }
    }
}
