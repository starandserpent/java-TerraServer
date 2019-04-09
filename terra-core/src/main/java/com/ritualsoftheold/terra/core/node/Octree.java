package com.ritualsoftheold.terra.core.node;

import com.ritualsoftheold.terra.core.material.TerraMaterial;

/**
 * Terra's octree. Underlying implementation might be pooled.
 *
 */
public interface Octree extends Node {
    
    Node getNodeAt(int index);
    
    Octree getOctreeAt(int index) throws ClassCastException;
    
    TerraMaterial getBlockAt(int index) throws ClassCastException;
    
    Chunk getChunkAt(int index) throws ClassCastException;
    
    void setNodeAt(int index, Node node);
    
    Node[] getNodes();
    
    void setNodes(Node[] nodes);
}
