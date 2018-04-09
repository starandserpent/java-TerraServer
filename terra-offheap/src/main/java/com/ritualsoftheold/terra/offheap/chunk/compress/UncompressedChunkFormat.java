package com.ritualsoftheold.terra.offheap.chunk.compress;

import com.ritualsoftheold.terra.buffer.BlockBuffer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk.ChangeIterator;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk.Storage;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

public class UncompressedChunkFormat implements ChunkFormat {
    
    public static final UncompressedChunkFormat INSTANCE = new UncompressedChunkFormat();
    
    private static final Memory mem = OS.memory();

    @Override
    public Storage convert(Storage origin, OffheapChunk.ChangeIterator changes, ChunkFormat format, ChunkBuffer.Allocator allocator) {
        
        
        return null; // Conversion not supported
    }

    @Override
    public Storage processQueries(OffheapChunk chunk, Storage storage, ChangeIterator changes) {
        long blocks = storage.address;
        
        while (changes.hasNext()) {
            changes.next();
            int index = changes.getIndex();
            int id = changes.getBlockId();
            
            mem.writeVolatileInt(blocks + index, id);
        }
        
        // This format can store everything
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
