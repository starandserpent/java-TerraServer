package com.ritualsoftheold.terra.node;

/**
 * Chunk is a node which contains many blocks.
 *
 */
public interface Chunk extends Node {
    
    Object getRef(int blockId);
    
    void setRef(int blockId, Object ref);
}
