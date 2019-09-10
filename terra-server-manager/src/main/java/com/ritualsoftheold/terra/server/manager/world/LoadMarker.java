package com.ritualsoftheold.terra.server.manager.world;

import com.ritualsoftheold.terra.core.chunk.ChunkLArray;
import com.ritualsoftheold.terra.server.manager.util.IntFlushList;

/**
 * Load markers are used by some world implementations to figure out
 * which parts to generate and keep loaded.
 * 
 * Implements comparable to allow sorting based on priority. Uses
 * {@link Integer#compare(int, int)} where first value is priority of this
 * and second is priority of the one that this is compared against.
 *
 */
public abstract class LoadMarker{
    
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

    protected LoadMarker(float x, float y, float z, float hardRadius, float softRadius) {
        move(x, y, z);
        IntFlushList octrees = new IntFlushList(64, 2); // TODO tune these settings
        this.hardRadius = hardRadius;
        this.softRadius = softRadius;
    }

    public void move(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    protected abstract void sendChunk(ChunkLArray chunk);

    protected abstract void sendPosition();

    protected abstract void init(String clientAddress, int clientPort, int streamid);

    public float getPosX() {
        return x;
    }

    public float getPosY() {
        return y;
    }

    public float getPosZ() {
        return z;
    }

    public float getHardRadius() {
        return hardRadius;
    }

    public float getSoftRadius() {
        return softRadius;
    }
}
