package com.ritualsoftheold.terra.node;

import com.ritualsoftheold.terra.buffer.BlockBuffer;

/**
 * Represents a world data type - might be a block, an octree or a chunk.
 *
 */
public interface Node {
    
    /**
     * Gets node type. Note that {@link Type#OTHER} means that
     * the enum does not have type, and you must use instanceof.
     * @return Node type.
     */
    Type getNodeType();
    
    public static enum Type {
        
        OCTREE,
        
        CHUNK,
        
        BLOCK,
        
        OTHER
    }
    
    /**
     * Accesses block buffer of this node if possible. Otherwise null is
     * returned. When a buffer is returned, it must be closed when user is done
     * with it; otherwise, Terra may eventually run out of memory.
     * 
     * <p>Block buffers returned by this method represent contents of the chunk
     * at their creation, or any time after that. Writes to buffers are not
     * necessarily visible in same buffer. They may also be queued in a way
     * that no one will immediately see the changes.
     * @return Block buffer or null.
     */
    BlockBuffer getBuffer();
}
