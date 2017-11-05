package com.ritualsoftheold.terra.offheap.chunk.compress;

import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkType;
import com.ritualsoftheold.terra.offheap.data.WorldDataFormat;

public interface ChunkFormat extends WorldDataFormat {
    
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

    void getBlocks(long chunk, int[] indices, short[] ids, int beginIndex, int endIndex);
    
    default short getBlock(long chunk, int index) {
        // This is simplistic/bad implementation. Please override for any serious usage
        
        int[] indices = new int[]{index};
        short[] ids = new short[1];
        getBlocks(chunk, indices, ids, 0, 1);
        
        return ids[0];
    }
    
    SetAllResult setAllBlocks(short[] data, ChunkBuffer.Allocator allocator);
    
    /**
     * Returned as a result for setAllBlocks call.
     *
     */
    public static class SetAllResult {
        
        public SetAllResult(long addr, int length) {
            this.addr = addr;
            this.length = length;
        }
        
        /**
         * Memory address where data is.
         */
        public long addr;
        
        /**
         * Length of data.
         */
        public int length;
    }
    
    int getChunkType();
    
    @Override
    default boolean isOctree() {
        return false;
    }
}
