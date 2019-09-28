package com.ritualsoftheold.terra.server;

import com.ritualsoftheold.terra.core.WorldLoadListener;
import com.ritualsoftheold.terra.core.chunk.ChunkLArray;
import com.ritualsoftheold.terra.core.markers.MovingMarker;
import com.ritualsoftheold.terra.server.morton.IntFlushList;

/**
 * Load markers are used by some world implementations to figure out
 * which parts to generate and keep loaded.
 * <p>
 * Implements comparable to allow sorting based on priority. Uses
 * {@link Integer#compare(int, int)} where first value is priority of this
 * and second is priority of the one that this is compared against.
 */
public abstract class LoadMarker extends MovingMarker implements WorldLoadListener {

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
        super(x, y, z);
        IntFlushList octrees = new IntFlushList(64, 2); // TODO tune these settings
        this.hardRadius = hardRadius;
        this.softRadius = softRadius;
    }

    @Override
    public void chunkLoaded(ChunkLArray chunk) {
        sendChunk(chunk);
    }

    @Override
    public void chunkUnloaded(ChunkLArray chunk) {

    }

    public abstract void sendChunk(ChunkLArray chunk);

    protected abstract boolean init(int id);

    public float getHardRadius() {
        return hardRadius;
    }

    public float getSoftRadius() {
        return softRadius;
    }
}
