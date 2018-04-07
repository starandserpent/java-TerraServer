package com.ritualsoftheold.terra.offheap.chunk.compress;

import com.ritualsoftheold.terra.buffer.BlockBuffer;
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
    OffheapChunk.Storage processQueries(OffheapChunk chunk, OffheapChunk.ChangeIterator changes);
    
    int getChunkType();
    
    BlockBuffer createBuffer(OffheapChunk chunk);
    
    @Override
    default boolean isOctree() {
        return false;
    }
}
