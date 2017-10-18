package com.ritualsoftheold.terra.offheap.io.dummy;

import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.io.ChunkLoader;

public class DummyChunkLoader implements ChunkLoader {

    @Override
    public ChunkBuffer loadChunks(int index, ChunkBuffer buf) {
        return buf;
    }

    @Override
    public ChunkBuffer saveChunks(int index, ChunkBuffer buf) {
        return buf;
    }

    @Override
    public int countBuffers() {
        return 0;
    }

}
