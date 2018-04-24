package com.ritualsoftheold.terra.offheap.chunk;

import com.ritualsoftheold.terra.buffer.BlockBuffer;
import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.material.TerraMaterial;
import com.ritualsoftheold.terra.offheap.chunk.compress.ChunkFormat;
import com.ritualsoftheold.terra.offheap.data.BufferWithFormat;
import com.ritualsoftheold.terra.offheap.data.CriticalBlockBuffer;
import com.ritualsoftheold.terra.offheap.data.MemoryAllocator;
import com.ritualsoftheold.terra.offheap.data.TypeSelector;
import com.ritualsoftheold.terra.offheap.data.WorldDataFormat;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk.Storage;

/**
 * Wraps a critical block buffer so that if there are too many materials,
 * format changes are gracefully handled.
 *
 */
public class WrappedCriticalBuffer implements BufferWithFormat, CriticalBlockBuffer {
    
    /**
     * Format of the underlying buffer.
     */
    private ChunkFormat format;
    
    /**
     * Underlying critical block buffer.
     */
    private BlockBuffer wrapped;
    
    /**
     * Storage information about the underlying buffer.
     */
    private Storage storage;
    
    /**
     * Memory allocator which we should use.
     */
    private MemoryAllocator allocator;
    
    /**
     * A way to select which chunk format is used next in case there are too
     * many materials.
     */
    private TypeSelector typeSelector;
    
    private MaterialRegistry materialRegistry;
    
    public WrappedCriticalBuffer(ChunkFormat format, BlockBuffer wrapped, Storage storage,
            MemoryAllocator allocator, TypeSelector typeSelector, MaterialRegistry registry) {
        this.format = format;
        this.wrapped = wrapped;
        this.storage = storage;
        this.allocator = allocator;
        this.typeSelector = typeSelector;
        this.materialRegistry = registry;
    }

    @Override
    public void close() {
        wrapped.close();
    }

    @Override
    public void seek(int index) {
        wrapped.seek(index);
    }
    
    @Override
    public int position() {
        return wrapped.position();
    }

    @Override
    public void next() {
        wrapped.next();
    }

    @Override
    public boolean hasNext() {
        return wrapped.hasNext();
    }

    @Override
    public void write(TerraMaterial material) {
        try {
            wrapped.write(material);
        } catch (TooManyMaterialsException e) {
            // Convert to different chunk format
            ChunkFormat nextFormat = (ChunkFormat) typeSelector.nextFormat(format); // Chunk -> octree is not possible here
            Storage newStorage = format.convert(storage, nextFormat, allocator);
            allocator.free(storage.address, storage.length);
            
            BlockBuffer newBuf = nextFormat.createCriticalBuffer(newStorage, materialRegistry);
            newBuf.seek(position());
            
            // Swap data to the new format
            format = nextFormat;
            storage = newStorage;
            wrapped = newBuf;
            
            // Apply change to new buf (some recursion, in worst case)
            wrapped.write(material);
        }
    }

    @Override
    public TerraMaterial read() {
        return wrapped.read();
    }

    @Override
    public Object readRef() {
        return wrapped.readRef();
    }

    @Override
    public void writeRef(Object ref) {
        wrapped.writeRef(ref);
    }

    @Override
    public WorldDataFormat getDataFormat() {
        return format;
    }

    @Override
    public Storage getStorage() {
        return storage;
    }
}
