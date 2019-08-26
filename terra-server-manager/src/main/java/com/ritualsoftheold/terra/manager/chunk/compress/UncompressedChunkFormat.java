package com.ritualsoftheold.terra.manager.chunk.compress;

import com.ritualsoftheold.terra.manager.material.Registry;
import com.ritualsoftheold.terra.manager.memory.MemoryAllocator;
import com.ritualsoftheold.terra.manager.DataConstants;
import com.ritualsoftheold.terra.manager.chunk.ChunkType;
import com.ritualsoftheold.terra.manager.data.BufferWithFormat;
import com.ritualsoftheold.terra.manager.data.CriticalBlockBuffer;
import com.ritualsoftheold.terra.manager.node.OffheapChunk;
import com.ritualsoftheold.terra.manager.node.OffheapChunk.ChangeIterator;
import com.ritualsoftheold.terra.manager.node.OffheapChunk.Storage;

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
            Registry registry) {
        throw new UnsupportedOperationException("TODO implement this");
    }
    
}
