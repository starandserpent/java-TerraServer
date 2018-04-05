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
     * Accesses block buffer of this node, if possible.
     * If it is not possible, null is returned.
     * @return Block buffer or null.
     */
    BlockBuffer getBuffer();
}
