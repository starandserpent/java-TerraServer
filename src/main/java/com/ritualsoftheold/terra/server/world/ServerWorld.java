package com.ritualsoftheold.terra.server.world;

import com.ritualsoftheold.terra.core.markers.Marker;
import com.ritualsoftheold.terra.core.TerraWorld;
import com.ritualsoftheold.terra.core.WorldLoadListener;
import com.ritualsoftheold.terra.core.materials.Registry;
import com.ritualsoftheold.terra.server.chunks.ChunkGenerator;
import com.ritualsoftheold.terra.server.LoadMarker;

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
    private Registry reg;

    public ServerWorld(int centerX, int centerY, int centerZ, ChunkGenerator generator, Registry reg, int worldSize) {
        loadMarkers = new ArrayList<>();
        this.reg = reg;

        chunkGenerator = new ChunkSVOGenerator(centerX, centerY, centerZ, worldSize, generator);
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
    public void updateMarker(Marker marker) {}

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
        // Starts initial generation
        if (marker instanceof LoadMarker) {
            LoadMarker loadMarker = (LoadMarker) marker;
            chunkGenerator.seekSector(loadMarker);
        }
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
     *
     * @param marker Load marker to update.
     * @param soft   If soft radius should be used.
     */
    public void updateLoadMarker(LoadMarker marker, boolean soft) {

    }
}
