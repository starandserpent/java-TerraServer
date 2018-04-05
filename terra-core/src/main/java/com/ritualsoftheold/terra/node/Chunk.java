package com.ritualsoftheold.terra.node;

import com.ritualsoftheold.terra.buffer.TerraRef;

/**
 * Chunk is a node which contains many blocks.
 *
 */
public interface Chunk extends Node {
    
    TerraRef createStaticRef(int size);
    
    TerraRef createDynamicRef(int initialSize);
}
