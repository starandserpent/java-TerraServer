package com.ritualsoftheold.terra.mesher.culling;

import com.jme3.scene.Geometry;

/**
 * Visual Terra object, be it chunk or octree.
 *
 */
public class VisualObject {
    
    /**
     * Represents if this object is a chunk
     * (instead of a single octree node).
     */
    public boolean isChunk;
    
    /**
     * Stores if the object is opaque. Note that chunks can be completely
     * opaque and single nodes can be partially transparent.
     */
    public boolean isOpaque;
    
    /**
     * Linked jME's geometry.
     */
    public Geometry linkedGeom;
    
    /**
     * Pointer to data.
     */
    public long addr;
    
    /**
     * Position modifier for this object to get to the edges. Usually this is
     * 0.5 * scale of the object.
     */
    public float posMod;
    
    public float posX, posY, posZ;
}
