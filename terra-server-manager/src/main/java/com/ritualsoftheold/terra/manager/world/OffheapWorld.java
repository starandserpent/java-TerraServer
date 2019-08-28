package com.ritualsoftheold.terra.manager.world;

import com.ritualsoftheold.terra.core.materials.Registry;
import com.ritualsoftheold.terra.core.octrees.OctreeBase;
import com.ritualsoftheold.terra.manager.WorldGeneratorInterface;
import com.ritualsoftheold.terra.manager.gen.objects.LoadMarker;
import com.ritualsoftheold.terra.manager.octree.OffheapOctree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Represents world that is mainly backed by offheap memory.
 */
public class OffheapWorld {
    
    // New world loader, no more huge methods in this class!
    private ChunkSVOGenerator chunkGenerator;
    private List<OffheapLoadMarker> loadMarkers;
    private WorldLoadListener listener;

    // Only used by the builder
    public OffheapWorld(WorldGeneratorInterface generator, Registry reg, int height, WorldLoadListener listener) {
        loadMarkers = new ArrayList<>();

        this.listener = listener;
        // Some cached stuff
        OffheapOctree masterOctree = new OffheapOctree();
        chunkGenerator = new ChunkSVOGenerator(generator, reg, height, masterOctree);
    }

    /**
     * Attempts to get an id for smallest node at given coordinates.
     * @return 32 least significant bits represent the actual id. 33th
     * tells if the id refers to chunk (1) or octree (2).
     */

    private OffheapLoadMarker checkLoadMarker(LoadMarker marker) {
        if (!(marker instanceof OffheapLoadMarker))
            throw new IllegalArgumentException("incompatible load marker");
        return (OffheapLoadMarker) marker;
    }

    public void addLoadMarker(LoadMarker marker) {
        loadMarkers.add(checkLoadMarker(marker));
        loadMarkers.sort(Comparator.reverseOrder()); // Sort most important first
    }
    

    public void removeLoadMarker(LoadMarker marker) {
        checkLoadMarker(marker); // Wrong type wouldn't actually break anything
        // But user probably wants to know if they're passing wrong marker to us
        
        Iterator<OffheapLoadMarker> it = loadMarkers.iterator();
        while (it.hasNext()) {
            OffheapLoadMarker m = it.next();
            if (m == marker) {
                it.remove();
                return;
            }
        }
    }

    public List<CompletableFuture<Void>> updateLoadMarkers() {
        List<CompletableFuture<Void>> pendingMarkers = new ArrayList<>(loadMarkers.size());
        // Delegate updating to async code, this might be costly
        for (OffheapLoadMarker marker : loadMarkers) {
            updateLoadMarker(marker, false);
        }
        
        return pendingMarkers;
    }
    
    public List<CompletableFuture<Void>> updateLoadMarkers(WorldLoadListener listener, boolean soft, boolean ignoreMoved) {
        List<CompletableFuture<Void>> pendingMarkers = new ArrayList<>(loadMarkers.size());
        // Delegate updating to async code, this might be costly
        for (OffheapLoadMarker marker : loadMarkers) {
            if (ignoreMoved || marker.hasMoved()) { // Update only marker that has been moved
                // When player moves a little, DO NOT, I repeat, DO NOT just blindly move load marker.
                // Move it when player has moved a few meters or so!
                pendingMarkers.add(CompletableFuture.runAsync(() -> updateLoadMarker(marker, soft)));
            }
        }
        
        return pendingMarkers;
    }

    //Debug method
    public void initialChunkGeneration(OffheapLoadMarker player, ArrayList<OctreeBase> nodes) {
        // Tell world loader to load stuff, and while doing so, update the load marker
//        chunkGenerator.seekSector(player.getX(), player.getZ(), player.getHardRadius()*2, worldListener, player);
        chunkGenerator.seekSector(player.getX(),player.getY(),player.getZ(),player.getHardRadius(), listener, nodes);
        player.markUpdated();
    }

    /**
     * Updates given load marker no matter what. Only used internally.
     * @param marker Load marker to update.
     * @param soft If soft radius should be used.
     */
    public void updateLoadMarker(OffheapLoadMarker marker, boolean soft) {
        // Tell world loader to load stuff, and while doing so, update the load marker
     chunkGenerator.updateSector(marker.getX(), marker.getZ(),
               soft ? marker.getSoftRadius() : marker.getHardRadius(), listener, marker);
        marker.markUpdated();
    }

    public OffheapLoadMarker createLoadMarker(float x, float y, float z, float hardRadius, float softRadius, int priority) {
        return new OffheapLoadMarker(x, y, z, hardRadius, softRadius, priority);
    }
}
