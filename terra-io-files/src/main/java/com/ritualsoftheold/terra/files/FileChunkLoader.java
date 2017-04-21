package com.ritualsoftheold.terra.files;

import java.nio.file.Path;

import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.io.ChunkLoader;

// TODO waiting for ChunkBuffer improvements
public class FileChunkLoader implements ChunkLoader {

    private Path dir;
    
    public FileChunkLoader(Path dir) {
        this.dir = dir;
    }
    
    @Override
    public ChunkBuffer loadChunks(short index, ChunkBuffer buf) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ChunkBuffer saveChunks(short index, ChunkBuffer buf) {
        // TODO Auto-generated method stub
        return null;
    }

}
