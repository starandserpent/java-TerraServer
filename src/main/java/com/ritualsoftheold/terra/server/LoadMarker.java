package com.ritualsoftheold.terra.server;

import com.ritualsoftheold.terra.core.DataConstants;
import com.ritualsoftheold.terra.core.WorldLoadListener;
import com.ritualsoftheold.terra.core.chunk.ChunkLArray;
import com.ritualsoftheold.terra.core.octrees.OffheapOctree;
import com.ritualsoftheold.terra.core.markers.MovingMarker;
import com.ritualsoftheold.terra.core.utils.CoreUtils;

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

    public abstract void sendOctree(OffheapOctree octree);

    protected abstract boolean init(int id);

    public void calculateMarkerOctants(float size) {

        int iterations = CoreUtils.calculateOctreeLayers((int) size);
        size = size * DataConstants.CHUNK_SCALE;

        for (int i = 0; i < iterations; i++) {
            int octant = CoreUtils.selectOctant(x, y, z, size);
            playerOctants.add(octant);
            size = size / 2;
        }
    }

    public ArrayList<Integer> getPlayerOctants() {
        return playerOctants;
    }

    public int getOctant(int index) {
        return playerOctants.get(index);
    }

    public float getHardRadius() {
        return hardRadius;
    }

    public float getSoftRadius() {
        return softRadius;
    }
}
