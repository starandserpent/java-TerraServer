package com.ritualsoftheold.terra.offheap.chunk.compress;

import com.ritualsoftheold.terra.offheap.Pointer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkType;
import com.ritualsoftheold.terra.offheap.data.WorldDataFormat;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;

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
    boolean convert(@Pointer long from, @Pointer long to, int type);
    
    /**
     * 
     * @param chunk
     * @param queue
     * @param size
     */
    void processQueries(OffheapChunk chunk, long queue, int size);

    void getBlocks(@Pointer long chunk, int[] indices, short[] ids, int beginIndex, int endIndex);
    
    default short getBlock(@Pointer long chunk, int index) {
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
        
        public SetAllResult(@Pointer long addr, int length) {
            this.addr = addr;
            this.length = length;
        }
        
        public SetAllResult(@Pointer long addr, int length, int typeSwap) {
            this.addr = addr;
            this.length = length;
            this.typeSwap = typeSwap;
        }
        
        /**
         * Memory address where data is.
         */
        public @Pointer long addr;
        
        /**
         * Length of data.
         */
        public int length;
        
        /**
         * If the format implementation decides that using given type is not
         * so great idea after all, it can change the type.
         */
        public int typeSwap = -1;
    }
    
    int getChunkType();
    
    @Override
    default boolean isOctree() {
        return false;
    }
}
