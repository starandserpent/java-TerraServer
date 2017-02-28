package com.ritualsoftheold.terra.node;

/**
 * Terra's octree. Underlying implementation might be pooled.
 *
 */
public interface Octree extends Node {
    
    Node getNodeAt(int index);
    
    Octree getOctreeAt(int index) throws ClassCastException;
    
    Block getBlockAt(int index) throws ClassCastException;
    
    Chunk getChunkAt(int index) throws ClassCastException;
    
    void setNodeAt(int index, Node node);
    
    Node[] getNodes();
    
    void setNodes(Node[] nodes);
    
    // Low level, optional API
    
    long l_getNodeAddr(int index);
    
    int l_getNodeAt(int index);
    
    void l_getData(int[] data);
}
