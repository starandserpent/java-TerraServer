package com.ritualsoftheold.terra.io.dummy;

import com.ritualsoftheold.terra.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.io.ChunkLoader;

public class DummyChunkLoader implements ChunkLoader {

    @Override
    public ChunkBuffer loadChunks(int index, ChunkBuffer buf) {
        return buf;
    }

    @Override
    public ChunkBuffer saveChunks(int index, ChunkBuffer buf) {
        return buf;
    }

}
