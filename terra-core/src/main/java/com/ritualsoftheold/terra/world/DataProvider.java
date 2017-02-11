package com.ritualsoftheold.terra.world;

import com.ritualsoftheold.terra.node.Chunk;
import com.ritualsoftheold.terra.node.Octree;

/**
 * Data provider for Terra's world. Implementations of it handle loading
 * and in some cases, saving of the world.
 * 
 * An example of load-and-save implementation would be flatfile based world
 * storage. An example of load-only implementation would be one which listens
 * at client side waiting for server to send world data and changes.
 *
 */
public interface DataProvider {
    
    /**
     * Provides master octree.
     * @return Master octree.
     */
    Octree masterOctree();
    
    /**
     * Provides octree with given index.
     * @param index Octree index.
     * @return Octree with given index.
     */
    Octree octree(long index);
    
    /**
     * Gets chunk with given index.
     * @param index Chunk index.
     * @return Chunk with given index.
     */
    Chunk chunk(long index);
    
    
    // Low level, optional API. Better performance, but not for general usage
    
    default void l_getOctree(long[] data, int uint_index) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
    
    default long l_getChunkPtr(int uint_index) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
    
    default void l_getChunk(long[] data, long ptr) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
