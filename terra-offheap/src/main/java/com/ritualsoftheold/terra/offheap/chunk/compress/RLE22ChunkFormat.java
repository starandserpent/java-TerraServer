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
    public short getBlock(long chunk, int index) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void getBlocks(long chunk, int[] indices, short[] ids) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setBlock(long chunk, int index, short id) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setBlocks(long chunk, int[] indices, short[] ids) {
        // TODO Auto-generated method stub
        
    }

}
