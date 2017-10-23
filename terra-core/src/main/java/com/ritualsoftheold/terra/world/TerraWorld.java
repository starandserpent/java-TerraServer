package com.ritualsoftheold.terra.world;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.node.Chunk;
import com.ritualsoftheold.terra.node.Node;
import com.ritualsoftheold.terra.node.Octree;

/**
 * Represents a single Terra world.
 *
 */
public interface TerraWorld {
    
    /**
     * Gets master octree of this world. Do not cache the result, as
     * it might change.
     * @return Master octree.
     */
    Octree getMasterOctree();
    
    /**
     * Gets material registry that is used with this world.
     * This method can be called without entering world.
     * @return Material registry.
     */
    MaterialRegistry getMaterialRegistry();
    
    Node getNode(float x, float y, float z);
    
    /**
     * Gets chunk at given location.
     * @param x X coordinate inside chunk.
     * @param y Y coordinate inside chunk.
     * @param z Z coordinate inside chunk.
     * @return Chunk at location.
     */
    Chunk getChunk(float x, float y, float z);
    
    /**
     * Adds load marker. User should make sure that
     * {@link #updateLoadMarkers()} is not in progress.
     * @param marker Load marker.
     */
    void addLoadMarker(LoadMarker marker);
    
    void removeLoadMarker(LoadMarker marker);
    
    /**
     * Requests load markers to be updated.
     * @return List of completable futures which need to be completed
     * before everything is loaded.
     */
    List<CompletableFuture<Void>> updateLoadMarkers();

}
