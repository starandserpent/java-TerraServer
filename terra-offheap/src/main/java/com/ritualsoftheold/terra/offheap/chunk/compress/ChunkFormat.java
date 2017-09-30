package com.ritualsoftheold.terra.offheap.chunk.compress;

import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkType;

public interface ChunkFormat {
    
    public static ChunkFormat forType(byte type) {
        switch (type) {
            case ChunkType.RLE_2_2:
                return RLE22ChunkFormat.INSTANCE;
            case ChunkType.EMPTY:
                throw new IllegalArgumentException("empty chunk type (TODO)");
            case ChunkType.UNCOMPRESSED:
                return UncompressedChunkFormat.INSTANCE;
            default:
                throw new IllegalArgumentException("unknown chunk type " + type);
        }
    }
    
    /**
     * Attempts to convert given chunk from type that this format
     * supports to another one.
     * @param from Source memory address.
     * @param to Target memory address.
     * @param type Chunk type.
     * @return If given type is unsupported, false is returned.
     */
    boolean convert(long from, long to, int type);
    
    /**
     * Processes given queries for given chunk. Note that chunk must have
     * type which this format is meant for.
     * @param chunk Address to chunk data.
     * @param chunkLen Length of allocated memory for chunk. If more than this is
     * needed, it can be requested from chunk buffer.
     * @param queue Address to query queue.
     * @param size Size of query data.
     */
    void processQueries(long chunk, int chunkLen, ChunkBuffer.Allocator alloc, long queue, int size);

    void getBlocks(long chunk, int[] indices, short[] ids);
}
