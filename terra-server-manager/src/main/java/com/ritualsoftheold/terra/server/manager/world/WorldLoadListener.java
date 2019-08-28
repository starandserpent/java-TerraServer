package com.ritualsoftheold.terra.server.manager.world;

import com.ritualsoftheold.terra.core.chunk.ChunkLArray;

/**
 * Methods in in this interface will be called when world data is loaded.
 *
 */
public interface WorldLoadListener {

    /**
     * This method is called when a chunk is loaded to storage.
     * @param chunk Temporary on-heap wrapper of the chunk. When this method
     * returns, it is not safe to use anymore.
     */
    void chunkLoaded(ChunkLArray chunk);
    

    void chunkUnloaded(ChunkLArray chunk);
}
