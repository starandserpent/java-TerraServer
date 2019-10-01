package com.ritualsoftheold.terra.server;

import com.ritualsoftheold.terra.core.WorldLoadListener;
import com.ritualsoftheold.terra.core.chunk.ChunkLArray;
import com.ritualsoftheold.terra.core.markers.MovingMarker;
import com.ritualsoftheold.terra.server.morton.IntFlushList;

import java.util.ArrayList;

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

    private ArrayList<Integer> playerOctants;

    protected LoadMarker(float x, float y, float z, float hardRadius, float softRadius) {
        super(x, y, z);
        IntFlushList octrees = new IntFlushList(64, 2); // TODO tune these settings
        this.hardRadius = hardRadius;
        this.softRadius = softRadius;
        playerOctants = new ArrayList<>();
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

    public void calculateMarkerOctants(float size) {
        if (x < size / 2 && y < size / 2 && z < size / 2) {
            // 1. Octant
            playerOctants.add(1);
        } else if (x > size / 2 && x < size && y < size / 2 && z < size / 2) {
            // 2. Octant
            playerOctants.add(2);
        } else if (x < size / 2 && y > size / 2 && y < size && z < size / 2) {
            // 3. Octant
            playerOctants.add(3);
        } else if (x > size / 2 && x < size && y > size / 2 && y < size && z < size / 2) {
            // 4. Octant
            playerOctants.add(4);
        } else if (x < size / 2 && y < size / 2 && z > size / 2 && z < size) {
            // 5. Octant
            playerOctants.add(5);
        } else if (x > size / 2 && x < size && y < size / 2 && z > size / 2 && z < size) {
            // 6. Octant
            playerOctants.add(6);
        } else if (x < size / 2 && y > size / 2 && y < size && z > size / 2 && z < size) {
            // 7. Octant
            playerOctants.add(7);
        } else {
            // 8. Octant
            playerOctants.add(8);
        }

        if (size > 16) {
            calculateMarkerOctants(size / 2);
        }
    }

    public float getHardRadius() {
        return hardRadius;
    }

    public float getSoftRadius() {
        return softRadius;
    }
}
