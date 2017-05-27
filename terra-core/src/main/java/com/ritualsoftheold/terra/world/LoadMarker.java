package com.ritualsoftheold.terra.world;

/**
 * Load markers are used by some world implementations to figure out
 * which parts to generate and keep loaded.
 *
 */
public class LoadMarker {
    
    /**
     * Coordinates for this marker.
     */
    private float x, y, z;
    
    private float hardRadius;
    
    private float softRadius;
    
    public LoadMarker(float x, float y, float z, float hardRadius, float softRadius) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.hardRadius = hardRadius;
        this.softRadius = softRadius;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }
    
    public void move(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    /**
     * World data must be kept loaded or loaded when its distance squared
     * to this marker is less than hard radius.
     * @return Hard radius (units squared).
     */
    public float getHardRadius() {
        return hardRadius;
    }
    
    /**
     * World data should usually be kept loaded when its distance squared
     * to this marker is less than soft radius.
     * @return Soft radius (units squared).
     */
    public float getSoftRadius() {
        return softRadius;
    }
}
