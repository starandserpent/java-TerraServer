package com.ritualsoftheold.terra.offheap.chunk.compress;

import com.ritualsoftheold.terra.buffer.BlockBuffer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkType;
import com.ritualsoftheold.terra.offheap.data.WorldDataFormat;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk.Storage;

public interface ChunkFormat extends WorldDataFormat {
    
    public static ChunkFormat forType(byte type) {
        switch (type) {
            case ChunkType.EMPTY:
                return EmptyChunkFormat.INSTANCE;
            case ChunkType.UNCOMPRESSED:
                return UncompressedChunkFormat.INSTANCE;
            case ChunkType.PALETTE16:
                return Palette16ChunkFormat.INSTANCE;
            default:
                throw new IllegalArgumentException("unknown chunk type " + type);
        }
    }
    
    Storage convert(Storage origin, OffheapChunk.ChangeIterator changes, ChunkFormat format, ChunkBuffer.Allocator allocator);
    
    OffheapChunk.Storage processQueries(OffheapChunk chunk, Storage storage, OffheapChunk.ChangeIterator changes);
    
    int getChunkType();
    
    BlockBuffer createBuffer(OffheapChunk chunk);
    
    @Override
    default boolean isOctree() {
        return false;
    }
}
