package com.ritualsoftheold.terra.offheap.chunk.iterator;

import com.ritualsoftheold.terra.offheap.chunk.ChunkType;

/**
 * Iterates over chunk data. Different implementations handle differnet
 * chunk formats.
 *
 */
public interface ChunkIterator {
    
    /**
     * Gets an iterator for given chunk type id. Throws an exception if no
     * applicable implementation was found.
     * @param addr Data address.
     * @param type Type flag.
     * @return Iterator instance.
     */
    public static ChunkIterator forChunk(long addr, byte type) {
        switch (type) {
        case ChunkType.RLE_2_2:
            return new RunLengthIterator(addr);
        case ChunkType.EMPTY:
            throw new IllegalArgumentException("iterating an empty chunk (type 1) is not possible");
        case ChunkType.UNCOMPRESSED:
            return new SimpleIterator(addr);
        default:
            throw new IllegalArgumentException("unknown chunk type " + type);
        }
    }
    
    /**
     * Takes next material and returns world id of it.
     * @return World id.
     */
    short nextMaterial();
    
    /**
     * Returns block count for current material.
     * @return Block count for current material.
     */
    int getCount();
    
    /**
     * Checks if the iterator has finished. Calling this method should be almost free.
     * @return Is this iterator done.
     */
    boolean isDone();
    
    /**
     * Resets this iterator to starting position with chunk it is iterating.
     */
    void reset();
    
    /**
     * Gets offset of current material's start.
     * @return Offset.
     */
    int getOffset();
}
