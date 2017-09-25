package com.ritualsoftheold.terra.offheap.chunk.compress;

import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer2;
import com.ritualsoftheold.terra.offheap.chunk.ChunkType;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

public class UncompressedChunkFormat implements ChunkFormat {
    
    private static final Memory mem = OS.memory();
    
    public static final UncompressedChunkFormat INSTANCE = new UncompressedChunkFormat();
    
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
    public void processQueries(long chunk, int chunkLen, ChunkBuffer2.Allocator alloc, long queue, int size) {
        long end = queue + size;
        for (long addr = queue; addr < end; addr += 8) {
            long query = mem.readVolatileLong(addr);
            
            int block = (int) (query >>> 16 & 0xffffff);
            short newId = (short) (query & 0xffff);
            
            mem.writeShort(chunk + block * 2, newId);
        }
    }

    @Override
    public void getBlocks(long chunk, int[] indices, short[] ids) {
        for (int i = 0; i < ids.length; i++) {
            ids[i] = mem.readShort(chunk + indices[i] * 2);
        }
    }
    
}
