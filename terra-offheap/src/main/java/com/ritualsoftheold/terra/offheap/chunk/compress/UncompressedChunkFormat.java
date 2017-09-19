package com.ritualsoftheold.terra.offheap.chunk.compress;

import com.ritualsoftheold.terra.offheap.chunk.ChunkType;

public class UncompressedChunkFormat implements ChunkFormat {
    
    @Override
    public boolean convert(long from, long to, int type) {
        switch (type) {
            case ChunkType.RLE_2_2:
                RunLengthCompressor.compress(from, to);
                break;
        }
        
        return false;
    }
    
}
