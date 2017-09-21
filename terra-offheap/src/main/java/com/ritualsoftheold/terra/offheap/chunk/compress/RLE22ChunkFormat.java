package com.ritualsoftheold.terra.offheap.chunk.compress;

import com.ritualsoftheold.terra.offheap.chunk.ChunkType;

public class RLE22ChunkFormat implements ChunkFormat {

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
    public void processQueries(long chunk, long queue, int size) {
        // TODO Auto-generated method stub
        
    }

}
