package com.ritualsoftheold.terra.server.manager.gen.objects;

/**
 * Load markers are used by some world implementations to figure out
 * which parts to generate and keep loaded.
 * 
 * Implements comparable to allow sorting based on priority. Uses
 * {@link Integer#compare(int, int)} where first value is priority of this
 * and second is priority of the one that this is compared against.
 *
 */
public class LoadMarker implements Comparable<LoadMarker> {
    
    /**
     * Coordinates for this marker.
     */
    private volatile float x, y, z;
    
    /**
     * The radius which this marker will force the world to be loaded.
     * Squared to avoid sqrt.
     */
    private final float hardRadius;
    
    /**
     * The radius in which this marker will make world to not be loaded.
     * Squared to avoid sqrt.
     */
    private final float softRadius;
    
    /**
     * If this marked has been processed after it moved last time.
     */
    private volatile boolean hasMoved;
    
    /**
     * Priority of the marker.
     */
    private final int priority;
    
    protected LoadMarker(float x, float y, float z, float hardRadius, float softRadius, int priority) {
        move(x, y, z);
        this.hardRadius = hardRadius;
        this.softRadius = softRadius;
        this.priority = priority;
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
        
        this.hasMoved = true;
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
    
    /**
     * Tells if this marker has been moved since last update.
     * @return Has this marker moved.
     */
    public boolean hasMoved() {
        return hasMoved;
    }
    
    /**
     * Marks this load marker as updated.
     */
    public void markUpdated() {
        hasMoved = false;
    }
    
    /**
     * Gets priority of this load marker. Higher means more important.
     * @return Priority.
     */
    public int getPriority() {
        return priority;
    }

    @Override
    public int compareTo(LoadMarker o) {
        return Integer.compare(priority, o.priority);
    }
}
