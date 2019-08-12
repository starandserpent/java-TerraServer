package com.ritualsoftheold.terra.offheap.chunk.compress;

import com.ritualsoftheold.terra.core.material.Registry;
import com.ritualsoftheold.terra.offheap.memory.MemoryAllocator;
import com.ritualsoftheold.terra.offheap.chunk.ChunkType;
import com.ritualsoftheold.terra.offheap.data.BufferWithFormat;
import com.ritualsoftheold.terra.offheap.data.CriticalBlockBuffer;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk.ChangeIterator;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk.Storage;

public class EmptyChunkFormat implements ChunkFormat {
    
    public static final EmptyChunkFormat INSTANCE = new EmptyChunkFormat();

    @Override
    public Storage convert(Storage origin, ChunkFormat format,
            MemoryAllocator allocator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Storage processQueries(OffheapChunk chunk, Storage storage,
            ChangeIterator changes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte getChunkType() {
        return ChunkType.EMPTY;
    }

    @Override
    public BufferWithFormat createBuffer(OffheapChunk chunk, Storage storage) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int newDataLength() {
        return 0;
    }

    @Override
    public CriticalBlockBuffer createCriticalBuffer(Storage storage,
            Registry registry) {
        throw new UnsupportedOperationException();
    }

}
