package com.ritualsoftheold.terra.server.manager.world;


import com.ritualsoftheold.foreman.main.ChunkSVOGenerator;
import com.ritualsoftheold.foreman.main.LoadMarker;
import com.ritualsoftheold.foreman.main.OffheapOctree;
import com.ritualsoftheold.foreman.main.WorldGeneratorInterface;
import com.ritualsoftheold.terra.core.markers.Marker;
import com.ritualsoftheold.terra.core.TerraWorld;
import com.ritualsoftheold.terra.core.WorldLoadListener;
import com.ritualsoftheold.terra.core.materials.Registry;
import com.ritualsoftheold.terra.core.octrees.OctreeBase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Represents world that is mainly backed by offheap memory.
 */
public class ServerWorld implements TerraWorld {
    
    // New world loader, no more huge methods in this class!
    private ChunkSVOGenerator chunkGenerator;
    private List<Marker> loadMarkers;
    private WorldLoadListener listener;
    private ArrayList<OctreeBase> octreeNodes;
    private Registry reg;

    // Only used by the builder
    public ServerWorld(WorldGeneratorInterface generator, Registry reg, int height, WorldLoadListener listener,
                       ArrayList<OctreeBase> octreeNodes) {
        loadMarkers = new ArrayList<>();
        this.octreeNodes = octreeNodes;
        this.reg = reg;
        this.listener = listener;
        // Some cached stuff
        OffheapOctree masterOctree = new OffheapOctree();
        chunkGenerator = new ChunkSVOGenerator(generator, reg, height, masterOctree);
    }

    @Override
    public void addMarker(Marker marker) {
        loadMarkers.add(marker);
    }

    @Override
    public void removeMarker(Marker marker) {
        loadMarkers.remove(marker);
    }

    @Override
    public void updateMarker(Marker marker) {

    }

    @Override
    public boolean checkMarker(Marker marker) {
        return false;
    }

    @Override
    public Registry getRegistry() {
        return reg;
    }

    @Override
    public void initialWorldGeneration(Marker marker) {
        // Tell world loader to load stuff, and while doing so, update the load marker
        if(marker instanceof LoadMarker) {
            LoadMarker loadMarker = (LoadMarker) marker;
            chunkGenerator.seekSector(loadMarker, octreeNodes);
        }
        /// chunkGenerator.seekSector(player.getX(),player.getY(),player.getZ(),player.getHardRadius(), listener, octreeNodes);
        //player.markUpdated();
    }

    public List<CompletableFuture<Void>> updateLoadMarkers(WorldLoadListener listener, boolean soft, boolean ignoreMoved) {
     /*   List<CompletableFuture<Void>> pendingMarkers = new ArrayList<>(loadMarkers.size());
        // Delegate updating to async code, this might be costly
        for (LoadMarker marker : loadMarkers) {
            if (ignoreMoved || marker.hasMoved()) { // Update only marker that has been moved
                // When player moves a little, DO NOT, I repeat, DO NOT just blindly move load marker.
                // Move it when player has moved a few meters or so!
                pendingMarkers.add(CompletableFuture.runAsync(() -> updateLoadMarker(marker, soft)));
            }
        }*/
        
       // return pendingMarkers;
        return null;
    }

    /**
     * Updates given load marker no matter what. Only used internally.
     * @param marker Load marker to update.
     * @param soft If soft radius should be used.
     */
    public void updateLoadMarker(LoadMarker marker, boolean soft) {


    }
}
