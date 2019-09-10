package com.ritualsoftheold.terra.server.manager.world;


import com.ritualsoftheold.terra.core.WorldLoadListener;
import com.ritualsoftheold.terra.core.materials.Registry;
import com.ritualsoftheold.terra.core.octrees.OctreeBase;
import com.ritualsoftheold.terra.server.manager.gen.interfaces.WorldGeneratorInterface;
import com.ritualsoftheold.terra.server.manager.octree.OffheapOctree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Represents world that is mainly backed by offheap memory.
 */
public class World implements TerraWorld{
    
    // New world loader, no more huge methods in this class!
    private ChunkSVOGenerator chunkGenerator;
    private List<LoadMarker> loadMarkers;
    private WorldLoadListener listener;
    private ArrayList<OctreeBase> octreeNodes;
    private Registry reg;

    // Only used by the builder
    public World(WorldGeneratorInterface generator, Registry reg, int height, WorldLoadListener listener,
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
    public void addLoadMarker(LoadMarker marker) {
        loadMarkers.add(marker);
    }

    @Override
    public void removeLoadMarker(LoadMarker marker) {
        Iterator<LoadMarker> it = loadMarkers.iterator();
        while (it.hasNext()) {
            LoadMarker m = it.next();
            if (m == marker) {
                it.remove();
                return;
            }
        }
    }

    public List<CompletableFuture<Void>> updateLoadMarkers() {
        List<CompletableFuture<Void>> pendingMarkers = new ArrayList<>(loadMarkers.size());
        // Delegate updating to async code, this might be costly
        for (LoadMarker marker : loadMarkers) {
            updateLoadMarker(marker, false);
        }
        
        return pendingMarkers;
    }

    @Override
    public Registry getRegistry() {
        return reg;
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

    //Debug method
    @Override
    public void initialWorldGeneration(LoadMarker player) {
        // Tell world loader to load stuff, and while doing so, update the load marker
//        chunkGenerator.seekSector(player.getX(), player.getZ(), player.getHardRadius()*2, worldListener, player);
       /// chunkGenerator.seekSector(player.getX(),player.getY(),player.getZ(),player.getHardRadius(), listener, octreeNodes);
        //player.markUpdated();
    }

    /**
     * Updates given load marker no matter what. Only used internally.
     * @param marker Load marker to update.
     * @param soft If soft radius should be used.
     */
    public void updateLoadMarker(LoadMarker marker, boolean soft) {
        // Tell world loader to load stuff, and while doing so, update the load marker
  //   chunkGenerator.updateSector(marker.getX(), marker.getZ(),
    //           soft ? marker.getSoftRadius() : marker.getHardRadius(), listener, marker);
      //  marker.markUpdated();

    }
}
