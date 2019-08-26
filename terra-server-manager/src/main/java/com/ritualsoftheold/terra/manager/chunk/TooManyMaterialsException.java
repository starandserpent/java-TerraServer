package com.ritualsoftheold.terra.manager.chunk;

import com.ritualsoftheold.terra.core.buffer.BlockBuffer;
import com.ritualsoftheold.terra.manager.material.TerraObject;
import com.ritualsoftheold.terra.manager.node.OffheapChunk;

/**
 * Can be thrown by {@link BlockBuffer#write(TerraObject) when the chunk
 * format cannot handle that many materials. This is caught by
 * {@link OffheapChunk.ChangeQueue} or in case of critical block buffers,
 * by {@link WrappedCriticalBuffer}.
 *
 */
public class TooManyMaterialsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TooManyMaterialsException() {
        super(null, null, false, false);
    }
}