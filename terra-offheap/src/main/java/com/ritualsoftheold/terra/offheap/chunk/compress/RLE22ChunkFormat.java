package com.ritualsoftheold.terra.offheap.chunk.compress;

import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer2;
import com.ritualsoftheold.terra.offheap.chunk.ChunkType;

public class RLE22ChunkFormat implements ChunkFormat {

    public static final RLE22ChunkFormat INSTANCE = new RLE22ChunkFormat();
    
    @Override
    public boolean convert(long from, long to, int type) {
        switch (type) {
            case ChunkType.UNCOMPRESSED:
                RunLengthCompressor.decompress(from, to);
                break;
        }
        
        return false;
    }

    @Override
    public void processQueries(long chunk, int chunkLen, ChunkBuffer2.Allocator buf, long queue, int size) {
        // TODO Auto-generated method stub
        
    }

}
