package com.ritualsoftheold.terra.offheap.chunk;

/**
 * Lists common chunk types in static final fields.
 *
 */
public class ChunkType {
    
    /**
     * RLE compressed chunk. 2 bytes for count, 2 for id.
     */
    public static final int RLE_2_2 = 0;
    
    /**
     * Empty chunk.
     */
    public static final int EMPTY = 1;
    
    /**
     * Uncompressed chunk. 2 bytes for id for ALL blocks.
     */
    public static final int UNCOMPRESSED = 2;
    
    /**
     * RLE compressed chunk. 3 bytes for count, 1 for id.
     */
    public static final int RLE_3_1 = 3;
    
    /**
     * RLE compressed chunk. 1 byte for both count and id.
     */
    public static final int RLE_1_1 = 4;
}
