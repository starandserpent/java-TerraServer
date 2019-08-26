package com.ritualsoftheold.terra.manager.chunk.compress;

import com.ritualsoftheold.terra.manager.material.Registry;
import com.ritualsoftheold.terra.manager.memory.MemoryAllocator;
import com.ritualsoftheold.terra.manager.chunk.ChunkType;
import com.ritualsoftheold.terra.manager.data.BufferWithFormat;
import com.ritualsoftheold.terra.manager.data.CriticalBlockBuffer;
import com.ritualsoftheold.terra.manager.node.OffheapChunk;
import com.ritualsoftheold.terra.manager.node.OffheapChunk.ChangeIterator;
import com.ritualsoftheold.terra.manager.node.OffheapChunk.Storage;

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
