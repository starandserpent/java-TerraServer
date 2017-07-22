package com.ritualsoftheold.terra.offheap.world;

import java.util.Set;
import com.ritualsoftheold.terra.node.Chunk;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;

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
    
    public void unload(long goal) {
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
        
        // Mark octrees to unload
        
        //
        long groups = world.getOctreeStorage().getGroups();
        for (int i = 0; i < 256; i += 8) {
            long groupAddr = mem.readLong(groups + i);
            
        }
    }
    
    /**
     * Performs actual unloading in world exclusive access.
     * @param goal
     * @param usedOctreeGroups
     * @param usedChunkBuffers
     */
    private void unloadCritical(long goal, LongSet usedOctreeGroups, Set<ChunkBuffer> usedChunkBuffers) {
        long stamp = world.enterExclusive();
        
        world.leaveExclusive(stamp);
    }
}
