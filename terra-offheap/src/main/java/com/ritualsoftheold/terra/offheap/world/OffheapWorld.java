package com.ritualsoftheold.terra.offheap.world;

import com.ritualsoftheold.terra.core.gen.interfaces.world.TerraWorld;
import com.ritualsoftheold.terra.offheap.WorldGeneratorInterface;
import com.ritualsoftheold.terra.core.gen.objects.LoadMarker;
import com.ritualsoftheold.terra.core.material.MaterialRegistry;
import com.ritualsoftheold.terra.core.node.Chunk;
import com.ritualsoftheold.terra.core.node.Node;
import com.ritualsoftheold.terra.core.node.Octree;
import com.ritualsoftheold.terra.offheap.node.OffheapOctree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Represents world that is mainly backed by offheap memory.
 */
public class OffheapWorld {
    // Some cached stuff
    private OffheapOctree masterOctree;
    private float masterScale;
    
    // World generatio

    // Coordinates of world center
    /*private float centerX = 0;
    private float centerY = 0;
    private float centerZ = 0;
    /*
     */
    
    // New world loader, no more huge methods in this class!
    private ChunkSVOGenerator chunkGenerator;
    private List<OffheapLoadMarker> loadMarkers;
    private MaterialRegistry reg;
    private WorldLoadListener worldListener;

    // Only used by the builder
    public OffheapWorld(WorldGeneratorInterface generator, MaterialRegistry reg, int height, WorldLoadListener worldListener) {
        this.reg = reg;
        this.worldListener = worldListener;
        loadMarkers = new ArrayList<>();
        chunkGenerator = new ChunkSVOGenerator(generator, reg, height);
        masterOctree = new OffheapOctree(reg);
    }

    public MaterialRegistry getMaterialRegistry() {
        return reg;
    }

    /**
     * Attempts to get an id for smallest node at given coordinates.
     * @param x X coordinate.
     * @param y Y coordinate.
     * @param z Z coordinate.
     * @return 32 least significant bits represent the actual id. 33th
     * tells if the id refers to chunk (1) or octree (2).
     */
    private long getNodeId(float x, float y, float z) {
        return 0; // TODO redo this
    }
    
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

    public void initialChunkGeneration(OffheapLoadMarker player) {
        // Tell world loader to load stuff, and while doing so, update the load marker
        chunkGenerator.seekSector(player.getX(), player.getZ(), player.getHardRadius(), worldListener, player);
        player.markUpdated();
    }

    /**
     * Updates given load marker no matter what. Only used internally.
     * @param marker Load marker to update.
     * @param soft If soft radius should be used.
     */
    public void updateLoadMarker(OffheapLoadMarker marker, boolean soft) {
        // Tell world loader to load stuff, and while doing so, update the load marker
//        chunkGenerator.updateSector(marker.getX(), marker.getZ(),
//                soft ? marker.getSoftRadius() : marker.getHardRadius(), worldListener, marker);
        marker.markUpdated();
    }

    /**
     * Sets default load listener for this world. It will be used when loading
     * world with TerraWorld's API; this implementation also allows you to
     * override the load listener.
     * @param listener Load listener.
     */
    
    /**
     * Updates master octree data from offheap data storage.
    public OffheapLoadMarker getLoadMarker(float x, float y, float z){
        for(OffheapLoadMarker loadMarker:loadMarkers){
            float lessX = loadMarker.getX() - loadMarker.getHardRadius() * 16;
            float moreX = loadMarker.getX() + loadMarker.getHardRadius() * 16;
            float lessZ = loadMarker.getZ() - loadMarker.getHardRadius() * 16;
            float moreZ = loadMarker.getZ() + loadMarker.getHardRadius() * 16;
            if(lessX < x && lessZ < z &&  moreX > x && moreZ > z){
               return loadMarker;
            }
        }
        return null;
    }

    /**
     * Gets current scale of master octree of this world.
     * @return Master octree scale.
     */
    public float getMasterScale() {
        return masterScale;
    }
    
    /**
     * Resulting data from chunk (potentially failed) copy operation.
     *
     */
    public static class CopyChunkResult {
        
        private byte type;
        private int length;
        private boolean success;
        
        public CopyChunkResult(boolean success, byte type, int length) {
            this.success = success;
            this.type = type;
            this.length = length;
        }
        
        /**
         * Checks if the copy operation was successful.
         * @return If it succeeded.
         */
        public boolean isSuccess() {
            return success;
        }
        
        /**
         * Gets the type of chunk.
         * @return Type of chunk.
         */
        public byte getType() {
            return type;
        }
        
        /**
         * Gets length of chunk data.
         * @return Length of chunk data.
         */
        public int getLength() {
            return length;
        }
    }
    
    /**
     * Copies chunk data for given chunk to given memory address.
     * Make sure there is enough space allocated!
     * @param id Chunk id.
     * @param target Target memory address
     * @return Relevant information about copied chunk.
     */
    /**
     * Attempts to copy data of given chunk. You'll provide a function which
     * can give memory addresses based on length of data.
     * @param id Chunk id.
     * @param handler Function to provide target memory address. It will
     * receive chunk data length as an argument. If it returns 0, copy
     * operation will fail (but no exception is thrown).
     * @return Relevant information about copied chunk, or indication of
     * failure to copy it.
     */
    /**
     * Copies an octree group or parts of one to given memory address.
     * Make sure there is enough space allocated!
     * are interpreted as last octree of the group
     * @return How much bytes were eventually copied.
    
    /**
     * Creates a new data verifier, configured to work with settings of this
     * world. It is not valid for any other worlds!
     * @return A new Terra verifier for this world.
     */

    public OffheapLoadMarker createLoadMarker(float x, float y, float z, float hardRadius, float softRadius, int priority) {
        return new OffheapLoadMarker(x, y, z, hardRadius, softRadius, priority);
    }
}
