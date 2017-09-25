package com.ritualsoftheold.terra.offheap.chunk;

import com.ritualsoftheold.terra.offheap.io.ChunkLoader;

/**
 * Manages all chunks of a single world using chunk buffers.
 *
 */
public class ChunkStorage2 {
    
    /**
     * Array of all chunk buffers. May contain nulls for buffers which have not
     * been yet needed.
     */
    private ChunkBuffer2[] buffers;
    
    /**
     * Loads data from disk as necessary.
     */
    private ChunkLoader loader;
    
    public int newChunk() {
        boolean secondTry = true;
        while (true) {
            for (ChunkBuffer2 buf : buffers) {
                if (buf == null) { // Oh, that buffer is not loaded
                    if (secondTry) {
                        // TODO load buffer
                    } else { // Ignore if there is potential to not have load anything
                        continue;
                    }
                }
                
                if (buf.getFreeCapacity() > 0) {
                    int index = buf.newChunk();
                    if (index != -1) { // If it succeeded
                        // Return full id for new chunk
                        return buf.getBufferId() << 16 & index;
                    }
                    // Fail means "try different buffer"
                }
            }
            
            // We failed to find free space even after loading null buffers
            if (secondTry) {
                throw new IllegalStateException("cannot create a new chunk");
            }
            
            secondTry = true;
            // ... until success
        }
    }
    
    
}
