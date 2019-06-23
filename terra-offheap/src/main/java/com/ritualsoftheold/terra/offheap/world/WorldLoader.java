package com.ritualsoftheold.terra.offheap.world;

import com.ritualsoftheold.terra.offheap.io.ChunkLoader;
import com.ritualsoftheold.terra.offheap.octree.OctreeStorage;
import com.ritualsoftheold.terra.offheap.world.gen.WorldGenManager;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.ChunkStorage;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Handles loading of offheap worlds. Usually this class is used by load
 * markers; direct usage is not recommended for application developers.
 */
public class WorldLoader {

    private static final Memory mem = OS.memory();

    /**
     * Reference to octree storage of the world this that operates with.
     */
    private OctreeStorage octreeStorage;

    /**
     * Reference to chunk storage of the world that this operates with.
     */
    private ChunkStorage chunkStorage;

    /**
     * Center coordinates of world. These will be moved when the world is
     * enlarged, and start at (0,0,0).
     */
    private float centerX, centerZ;

    /**
     * Full id of master octree.
     */
    private int masterOctree;

    /**
     * Scale of the whole world.
     */
    private float worldScale;

    /**
     * World generation manager. Mandatory if this loader ever needs to
     * generate stuff, otherwise not needed.
     */
    private WorldGenManager genManager;

    /**
     * Initializes a new world loader.
     *
     * @param octreeStorage Octree storage of the world.
     * @param chunkStorage  Chunk storage of the world.
     * @param genManager    World generation manager.
     */
    public WorldLoader(OctreeStorage octreeStorage, ChunkStorage chunkStorage, WorldGenManager genManager) {
        this.octreeStorage = octreeStorage;
        this.chunkStorage = chunkStorage;
        this.genManager = genManager;
    }

    /**
     * Sets world center, master octree and world scale.
     *
     * @param octree Master octree index.
     * @param scale  Scale of the world.
     */
    public void worldConfig(int octree, float scale) {
        this.masterOctree = octree;
        this.worldScale = scale;
    }

    public void seekSector(float x, float z, float range, WorldLoadListener listener, OffheapLoadMarker trigger) {
        this.centerX = 0;
        this.centerZ = 0;

        for(float rangeY = -range*16; rangeY < range*16; rangeY +=16) {
            loadArea(0, rangeY, 0, listener,  trigger);
            for (float f = 1; f <= range; f++) {
                for (float rangeZ = -f; rangeZ < f; rangeZ++) {
                    loadArea(-16 * rangeZ, rangeY, -16 * f, listener, trigger);
                    loadArea(16 * f, rangeY, 16 * rangeZ, listener, trigger);
                }
                for (float rangeX = -f; rangeX < f; rangeX++) {
                    loadArea(-16 * f, rangeY, -16 * rangeX, listener, trigger);
                    loadArea(16 * rangeX, rangeY, 16 * f, listener, trigger);
                }
                loadArea(16 * f, rangeY,16 * f , listener, trigger);
                loadArea(-16 * f, rangeY,  -16 * f , listener, trigger);
            }
        }
    }

    public void updateSector(float x, float z, float range, WorldLoadListener listener, OffheapLoadMarker trigger) {

        for(float rangeY = -range*16; rangeY < range*16; rangeY +=16) {
            if (x > centerX) {
                for (float rangeZ = -range; rangeZ <= range; rangeZ++) {
                    loadArea(16 * range + x, rangeY, 16 * rangeZ + z, listener, trigger);
                }
                for (float rangeX = -range; rangeX <= range; rangeX++) {
                    unloadArea(-16 * range + centerX, rangeY, -16 * rangeX + centerZ, listener, trigger);
                }
            } else if (x < centerX) {
                for (float rangeZ = -range; rangeZ <= range; rangeZ++) {
                    loadArea(-16 * range + x, rangeY, -16 * rangeZ + z, listener, trigger);
                }
                for (float rangeX = -range; rangeX <= range; rangeX++) {
                    unloadArea(16 * range + centerX, rangeY, 16 * rangeX + centerZ, listener, trigger);
                }
            }

            if (z > centerZ) {
                for (float rangeZ = -range; rangeZ <= range; rangeZ++) {
                    unloadArea(-16 * rangeZ + centerX, rangeY, -16 * range + centerZ, listener, trigger);
                }
                for (float rangeX = -range; rangeX <= range; rangeX++) {
                    loadArea(16 * rangeX + x, rangeY, 16 * range + z, listener, trigger);
                }
            } else if (z < centerZ) {
                for (float rangeX = -range; rangeX <= range; rangeX++) {
                    loadArea(-16 * rangeX + x, rangeY, -16 * range + z, listener, trigger);
                }
                for (float rangeZ = -range; rangeZ <= range; rangeZ++) {
                    unloadArea(16 * rangeZ + centerX, rangeY, 16 * range + centerZ, listener, trigger);
                }
            }
        }
        centerX = x;
        centerZ = z;
    }

    public void unloadArea(float x, float y, float z, WorldLoadListener listener, OffheapLoadMarker trigger){
        ChunkLoader chunkLoader = new ChunkLoader(listener);
        OffheapChunk chunk = chunkLoader.getChunk(x, y, z, trigger);
        if(chunk != null) {
            //genManager.remove(chunk);
            listener.chunkUnloaded(chunk);
        }
    }

    public void loadArea(float x, float y, float z, WorldLoadListener listener, OffheapLoadMarker trigger) {
        if(x > 0 && z > 0) {
            OffheapChunk chunk = genManager.generate(x,  y, z);
            trigger.addBuffer(chunk.getChunkBuffer());
            listener.chunkLoaded(chunk);
        }
    }
    
    /**
     * Seeks the smallest possible octree in the world which can hold
     * area with given center and range. After that, it is loaded.
     * @param x X coordinate of area center.
     * @param y Y coordinate of area center.
     * @param z Z coordinate of area center.
     * @param range Range of area.
     * @param listener World load listener, which is notified for everything
     * that is loaded.
     * @param generate If new terrain should be generated.
     * @param trigger Load marker that triggered this operation or null.
     */
    public void seekArea(float x, float y, float z, float range, WorldLoadListener listener, boolean generate, OffheapLoadMarker trigger) {
        /**
         * Node coordinates, start at world center.
         */
        float nodeX, nodeZ;
        
        /**
         * Relative coordinates to current node. Starting from relative
         * to center of world.
         */
        float rX,  rZ;
        
        /**
         * Scale of node that is currently operated
         */
        float scale;
        
        // Update/create some values we need later on
        nodeX = centerX;
        nodeZ = centerZ;
        
        rX = x - centerX;
        rZ = z - centerZ;
        
        scale = worldScale;
        
        /**
         * Id of current node, starts as master octree.
         */
        int nodeId = masterOctree;
        
        while (true) {
            // Figure out some stuff about current node
            octreeStorage.markUsed(nodeId >>> 24);
            long addr = octreeStorage.getOctreeAddr(nodeId);
            
            byte flags = mem.readVolatileByte(addr); // Tells information about child nodes
            // (flags >>> index & 1): 1 when octree/chunk/"octree null", 0 when single node
            
            // Lookup table, in which child node (relative) coordinates fall in
            // For efficiency, mutate node AND relative coordinates at the same time :)
            int index = 0;
            float posMod = 0.25f * scale; // posMod is always scale / 4
            float pos2Mod = 2 * posMod;
            // Reduce coordinates now, then add doubly to them if needed!
            float subNodeX = nodeX - posMod;
            float subNodeZ = nodeZ - posMod;
            // Increase coordinates now, then reduce them doubly if needed
            rX += posMod;
            rZ += posMod;
            
            if (rX > 0) {
                index += 1;
                subNodeX += pos2Mod;
                rX -= pos2Mod;
            }
            if (rZ > 0) {
                index += 4;
                subNodeZ += pos2Mod;
                rZ -= pos2Mod;
            }
            
            // Now that we know the node, fetch data from it
            long nodeAddr = addr + index * 4;
            int node = mem.readVolatileInt(nodeAddr);
            int flag = flags >>> index & 1;
            
            // Check if additional loading is necessary...
            if (flag == 0) { // ... or if it is a single node after all
                break; // No further action necessary
            } else if (node == 0) { // && flag == 1, naturally
                if (!generate) { // Generation is disallowed
                    // But we would need to generate to move deeper...
                    break;
                }
                
                // Ok, this is something that has not been generated, so it is
                // "octree null": flag is 1, but node is 0 (kind of null pointer)
                if (scale == DataConstants.CHUNK_SCALE) {
                 //   genManager.generate(addr, index, subNodeX, subNodeZ, scale);
                } else {
                    //System.out.println("Will create octree...");
                    node = octreeStorage.newOctree(); // Create octree and attempt to swap it
                    if (!mem.compareAndSwapInt(nodeAddr, 0, node)) {
                        // Someone was quicker. Use their version, then
                        node = mem.readVolatileInt(nodeAddr);
                        // TODO deal with the trash
                    }
                }
            }
            
            // Prepare various variables for next cycle
            scale *= 0.5;  // Scale essentially becomes posMod
            if (scale == DataConstants.CHUNK_SCALE) { // Dereference a chunk
                chunkStorage.ensureAndKeepLoaded(node); // Use node content as chunk id and load it
                node = mem.readVolatileInt(nodeAddr);
                
                OffheapChunk chunk = chunkStorage.getChunkInternal(node);
                listener.chunkLoaded(chunk);
                //trigger.addBuffer(chunk.getChunkBuffer()); // Add to load marker

                break; // No further action necessary
            } else { // "Dereference" an octree
                nodeId = node; // New node id to content of current node
            }
            
            // Check if range is small enough to fit in that child node with our coordinates
            if (rX + range > subNodeX + posMod || rX - range < subNodeX - posMod // X coordinate
                    || rZ + range > subNodeZ + posMod || rZ - range < subNodeZ - posMod) { // Z coordinate
                // When ANY fails: we have found the node over which we must load all
                //loadArea(x, y, z, range, nodeId, scale, nodeX, nodeZ, listener, generate, trigger);
                break;
            }
            
            // If we are still going to continue, replace node coordinates with subnode coordinates
            nodeX = subNodeX;
            nodeZ = subNodeZ;
            
            // And since this is not master octree anymore, remember to fire an event
          //  listener.octreeLoaded(addr, octreeStorage.getGroup(nodeId >>> 24), nodeId, nodeX, nodeZ, scale, trigger);
            //trigger.addOctree(nodeId, scale, nodeX, nodeY, nodeZ); // Add to load marker
        }
        
        // Finally, tell listener we are done (mainly to support network batching)
        listener.finished(trigger);
    }
    
    /**
     * Loads given node and its children which are closer than given range
     * from the center of the area. Note that node center and area center
     * are probably different. If you do not have a node if and its center
     * coordinates, use {@link #(float, float, float, float, WorldLoadListener, boolean)}
     * instead; it will call this method once it has figured those out.
     * @param x X coordinate of center of area.
     *  @param z Z coordinate of center of area.
     * @param range Range of area.
     * @param nodeId Node id. It will be first node that is loaded, and some of
     * its child nodes and their children will be loaded.
     * @param scale Scale of node where to start loading.
     * @param nodeX Node center X coordinate.
     * @param nodeY Node center Y coordinate.
     * @param nodeZ Node center Z coordinate.
     * @param listener World load listener.
     * @param generate If new terrain should be generated when needed.
     * @param trigger Load marker that triggered this operation or null.
     */
    public void loadArea(float x, float z, float range, int nodeId, float scale,
            float nodeX, float nodeY, float nodeZ, WorldLoadListener listener, boolean generate, OffheapLoadMarker trigger) {
        //System.out.println("loadArea, scale: " + scale);
        // Fetch data about given node
        octreeStorage.markUnused(nodeId >>> 24);
        long addr = octreeStorage.getOctreeAddr(nodeId); // This also loads the octree with group it is in
        //System.out.println("loadArea address: " + address);
        byte flags = mem.readVolatileByte(addr);
        addr += 1; // Skip the flags to data
        
        // Fire event to listener
        listener.octreeLoaded(addr, octreeStorage.getGroup(nodeId >>> 24), nodeId, nodeX, nodeY, nodeZ, scale, trigger);
        
        // Operate with scale of child nodes
        scale *= 0.5f;
        float posMod = 0.5f * scale; // Parameter scale / 4
        
        // Loop through child nodes
        for (int i = 0; i < 8; i++) {
            int flag = flags >>> i & 1;
            //System.out.println("flag == " + flag + ", scale == " + scale);
            
            // Octree or chunk
            if (flag == 1) {
                // Create coordinates for child node
                float subNodeX = 0;
                float subNodeY = 0;
                float subNodeZ = 0;
                switch (i) { // (with a lookup table)
                    case 0:
                        subNodeX = nodeX - posMod;
                        subNodeY = nodeY - posMod;
                        subNodeZ = nodeZ - posMod;
                        break;
                    case 1:
                        subNodeX = nodeX + posMod;
                        subNodeY = nodeY - posMod;
                        subNodeZ = nodeZ - posMod;
                        break;
                    case 2:
                        subNodeX = nodeX - posMod;
                        subNodeY = nodeY + posMod;
                        subNodeZ = nodeZ - posMod;
                        break;
                    case 3:
                        subNodeX = nodeX + posMod;
                        subNodeY = nodeY + posMod;
                        subNodeZ = nodeZ - posMod;
                        break;
                    case 4:
                        subNodeX = nodeX - posMod;
                        subNodeY = nodeY - posMod;
                        subNodeZ = nodeZ + posMod;
                        break;
                    case 5:
                        subNodeX = nodeX + posMod;
                        subNodeY = nodeY - posMod;
                        subNodeZ = nodeZ + posMod;
                        break;
                    case 6:
                        subNodeX = nodeX - posMod;
                        subNodeY = nodeY + posMod;
                        subNodeZ = nodeZ + posMod;
                        break;
                    case 7:
                        subNodeX = nodeX + posMod;
                        subNodeY = nodeY + posMod;
                        subNodeZ = nodeZ + posMod;
                        break;
                }
                
                // Check if we should load this (range)
                float hitRange = range + scale;
                if (Math.abs(subNodeX - x) > hitRange
                        || Math.abs(subNodeZ - z) > hitRange) {
                    continue; // Range exceeded, do not load
                }
                
                long nodeAddr = addr + i * 4;
                int node = mem.readVolatileInt(nodeAddr);
                
                // Check if node exists, and create if it doesn't
                if (node == 0) {
                    if (!generate) { // Generation is disallowed
                        continue;
                    }
                    
                    // Ok, this is something that has not been generated, so it is
                    // "octree null": flag is 1, but node is 0 (kind of null pointer)
                    if (scale == DataConstants.CHUNK_SCALE) {                        
                        //System.out.println("Create chunk (i: " + i + ")");
                   //     genManager.generate(addr, i, subNodeX, subNodeZ, scale);
                        node = mem.readVolatileInt(nodeAddr); // Read node, whatever it is
                    } else {
                        node = octreeStorage.newOctree(); // Create octree and attempt to swap it
                        //System.out.println("Create octree!");
                        if (!mem.compareAndSwapInt(nodeAddr, 0, node)) {
                            // Someone was quicker. Use their version, then
                            node = mem.readVolatileInt(nodeAddr);
                            // TODO deal with the trash
                        }
                    }
                }
                
                if (scale == DataConstants.CHUNK_SCALE) { // It is a chunk!
                    //System.out.println("Chunk path...");
                    // Load chunk and then fire event to listener
                    chunkStorage.ensureAndKeepLoaded(node);
                    OffheapChunk chunk = chunkStorage.getChunkInternal(node);
                    chunk.setCoordinates(subNodeX, subNodeY, subNodeZ);
                    listener.chunkLoaded(chunk);
                 //   trigger.addBuffer(chunk.getChunkBuffer()); // Add to load marker
                } else { // Octree. Here comes recursion...
                    // TODO multithreading
                    //System.out.println("Octree path, node: " + node);
                    loadArea(x, z, range, node, scale, subNodeX, subNodeY, subNodeZ, listener, generate, trigger);
                }
            } // else: no action needed, single node was loaded with getOctreeAddr
            //System.out.println("end of i: " + i);
        }
    }
}
