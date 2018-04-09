package com.ritualsoftheold.terra.offheap.chunk.compress;

import com.ritualsoftheold.terra.buffer.BlockBuffer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkType;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer.Allocator;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk.ChangeIterator;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk.Storage;

public class EmptyChunkFormat implements ChunkFormat {
    
    public static final EmptyChunkFormat INSTANCE = new EmptyChunkFormat();

    @Override
    public Storage convert(Storage origin, ChunkFormat format, Allocator allocator) {
        return null; // Conversion FAILED
    }

    @Override
    public Storage processQueries(OffheapChunk chunk, Storage storage, ChangeIterator changes) {
        throw new UnsupportedOperationException("empty chunk");
    }

    @Override
    public int getChunkType() {
        return ChunkType.EMPTY;
    }

    @Override
    public BlockBuffer createBuffer(OffheapChunk chunk) {
        return null; // Nope!
    }

}
