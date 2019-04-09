package com.ritualsoftheold.terra.chunk.compress;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.memory.MemoryAllocator;
import com.ritualsoftheold.terra.DataConstants;
import com.ritualsoftheold.terra.chunk.ChunkType;
import com.ritualsoftheold.terra.data.BufferWithFormat;
import com.ritualsoftheold.terra.data.CriticalBlockBuffer;
import com.ritualsoftheold.terra.node.OffheapChunk;
import com.ritualsoftheold.terra.node.OffheapChunk.ChangeIterator;
import com.ritualsoftheold.terra.node.OffheapChunk.Storage;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

public class UncompressedChunkFormat implements ChunkFormat {
    
    public static final UncompressedChunkFormat INSTANCE = new UncompressedChunkFormat();
    
    private static final Memory mem = OS.memory();

    @Override
    public Storage convert(Storage origin, ChunkFormat format, MemoryAllocator allocator) {
        throw new UnsupportedOperationException("TODO"); // TODO implement conversion at some point
    }

    @Override
    public Storage processQueries(OffheapChunk chunk, Storage storage, ChangeIterator changes) {
        while (changes.hasNext()) {
            changes.next();
            int index = changes.getIndex();
            int id = changes.getBlockId();
            
            storage.writeVolatileInt(index * 4, id);
        }
        
        // This format can store everything
        return null;
    }

    @Override
    public byte getChunkType() {
        return ChunkType.UNCOMPRESSED;
    }


    @Override
    public BufferWithFormat createBuffer(OffheapChunk chunk, Storage storage) {
        throw new UnsupportedOperationException("TODO implement this");
    }

    @Override
    public int newDataLength() {
        return DataConstants.CHUNK_MAX_BLOCKS * 4;
    }

    @Override
    public CriticalBlockBuffer createCriticalBuffer(Storage storage,
            MaterialRegistry materialRegistry) {
        throw new UnsupportedOperationException("TODO implement this");
    }
    
}
