package com.ritualsoftheold.terra.node;

/**
 * Terra's octree. Underlying implementation might be pooled.
 *
 */
public interface Octree extends Node {
    
    Node getNodeAt(int index);
    
    Octree getOctreeAt(int index);
    
    Block getBlockAt(int index);
    
    Chunk getChunkAt(int index);
    
    Node[] getNodes();
}
