package com.ritualsoftheold.terra.offheap.chunk.compress;

import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
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
    public void processQueries(long chunk, int chunkLen, ChunkBuffer.Allocator alloc, long queue, int size) {
        long end = queue + size;
        for (long addr = queue; addr < end; addr += 8) {
            long query = mem.readVolatileLong(addr);
            
            int block = (int) (query >>> 16 & 0xffffff);
            short newId = (short) (query & 0xffff);
            
            mem.writeShort(chunk + block * 2, newId);
        }
    }

    @Override
    public void getBlocks(long chunk, int[] indices, short[] ids, int beginIndex, int endIndex) {
        for (int i = beginIndex; i < endIndex; i++) {
            ids[i] = mem.readShort(chunk + indices[i] * 2);
        }
    }

    @Override
    public ChunkFormat.SetAllResult setAllBlocks(short[] data, ChunkBuffer.Allocator allocator) {
        long addr = allocator.alloc(DataConstants.CHUNK_UNCOMPRESSED); // Allocate enough memory
        
        // Copy data there
        for (int i = 0; i < data.length; i++) {
            mem.writeShort(addr + i * 2, data[i]);
        }
        
        return new ChunkFormat.SetAllResult(addr, DataConstants.CHUNK_UNCOMPRESSED);
    }

    @Override
    public int getChunkType() {
        return ChunkType.UNCOMPRESSED;
    }
    
}
