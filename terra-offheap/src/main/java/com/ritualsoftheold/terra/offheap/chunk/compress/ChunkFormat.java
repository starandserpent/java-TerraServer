package com.ritualsoftheold.terra.offheap.chunk.compress;

import com.ritualsoftheold.terra.offheap.chunk.iterator.ChunkIterator;


public interface ChunkFormat {
    
    public static ChunkIterator forType(byte type) {
        switch (type) {
            default:
                throw new IllegalArgumentException("unknown chunk type " + type);
        }
    }
    
    /**
     * Attempts to convert given chunk from type that this compressor
     * supports to another one.
     * @param from Source memory address.
     * @param to Target memory address.
     * @param type Chunk type.
     * @return If given type is unsupported, false is returned.
     */
    boolean convert(long from, long to, int type);
    
    short getBlock(long chunk, int index);
    
    void getBlocks(long chunk, int[] indices, short[] ids);
    
    void setBlock(long chunk, int index, short id);
    
    void setBlocks(long chunk, int[] indices, short[] ids);
}
