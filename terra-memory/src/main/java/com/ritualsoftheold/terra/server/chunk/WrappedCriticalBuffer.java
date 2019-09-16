package com.ritualsoftheold.terra.server.chunk;

import com.ritualsoftheold.terra.core.BlockBuffer;
import com.ritualsoftheold.terra.core.materials.Registry;
import com.ritualsoftheold.terra.server.MemoryAllocator;
import com.ritualsoftheold.terra.server.data.TypeSelector;

/**
 * Wraps a critical block buffer so that if there are too many materials,
 * format changes are gracefully handled.
 * @see TooManyMaterialsException The exception we catch.
 *
 */
public class WrappedCriticalBuffer {
    
    /**
     * Format of the underlying buffer.
     */
    
    /**
     * Underlying critical block buffer.
     */
    private BlockBuffer wrapped;
    
    /**
     * Storage information about the underlying buffer.
     */
    
    /**
     * Memory allocator which we should use.
     */
    private MemoryAllocator allocator;
    
    /**
     * A way to select which chunk format is used next in case there are too
     * many materials.
     */
    private TypeSelector typeSelector;
    
    private Registry registry;
    
    public WrappedCriticalBuffer(BlockBuffer wrapped,
            MemoryAllocator allocator, TypeSelector typeSelector, Registry registry) {
        this.wrapped = wrapped;
        this.allocator = allocator;
        this.typeSelector = typeSelector;
        this.registry = registry;
    }

    public void close() {
        wrapped.close();
    }

    public void seek(int index) {
        wrapped.seek(index);
    }
    
    public int position() {
        return wrapped.position();
    }

    public void next() {
        wrapped.next();
    }

    public boolean hasNext() {
        return wrapped.hasNext();
    }

    public Object readRef() {
        return wrapped.readRef();
    }


    public void writeRef(Object ref) {
        wrapped.writeRef(ref);
    }
}
