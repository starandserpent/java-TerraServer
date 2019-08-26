package com.ritualsoftheold.terra.net.client;

import com.ritualsoftheold.terra.manager.io.ChunkLoader;
import com.ritualsoftheold.terra.manager.chunk.ChunkBuffer;

public class NetChunkLoader implements ChunkLoader {

    @Override
    public ChunkBuffer loadChunks(int index, ChunkBuffer buf) {
        return buf; // Chunks are coming soon over network
    }

    @Override
    public ChunkBuffer saveChunks(int i, ChunkBuffer buf) {
        return buf; // Client side saving makes no sense
    }

}
