package com.ritualsoftheold.terra.offheap.chunk.compress;

import com.ritualsoftheold.terra.buffer.BlockBuffer;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk.ChangeIterator;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk.Storage;

public class EmptyChunkFormat implements ChunkFormat {
    
    public static final EmptyChunkFormat INSTANCE = new EmptyChunkFormat();

    @Override
    public boolean convert(long from, long to, int type) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Storage processQueries(OffheapChunk chunk, ChangeIterator changes) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getChunkType() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public BlockBuffer createBuffer(OffheapChunk chunk) {
        // TODO Auto-generated method stub
        return null;
    }

}
