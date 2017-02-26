package com.ritualsoftheold.terra.node;

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
    
    // Low level, optional API
    
    default long l_getAddress() {
        throw new UnsupportedOperationException();
    }
    
    default int l_getDataSize() {
        throw new UnsupportedOperationException();
    }
}
