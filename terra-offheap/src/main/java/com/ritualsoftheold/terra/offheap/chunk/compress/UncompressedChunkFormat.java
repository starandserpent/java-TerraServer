package com.ritualsoftheold.terra.offheap.chunk.compress;

import com.ritualsoftheold.terra.offheap.chunk.ChunkType;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

public class UncompressedChunkFormat implements ChunkFormat {
    
    private static final Memory mem = OS.memory();
    
    @Override
    public boolean convert(long from, long to, int type) {
        switch (type) {
            case ChunkType.RLE_2_2:
                RunLengthCompressor.compress(from, to);
                break;
        }
        
        return false;
    }

    @Override
    public short getBlock(long chunk, int index) {
        return mem.readVolatileShort(chunk + index * 2);
    }

    @Override
    public void getBlocks(long chunk, int[] indices, short[] ids) {
        for (int i = 0; i < indices.length; i++) {
            ids[i] = getBlock(chunk, i);
        }
    }

    @Override
    public void setBlock(long chunk, int index, short id) {
        mem.writeVolatileShort(chunk + index * 2, id);
    }

    @Override
    public void setBlocks(long chunk, int[] indices, short[] ids) {
        for (int i = 0; i < indices.length; i++) {
            setBlock(chunk, i, ids[i]);
        }
    }
    
}
