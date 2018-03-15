package com.ritualsoftheold.terra.offheap.world;

import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.ChunkStorage;
import com.ritualsoftheold.terra.offheap.octree.OctreeStorage;
import com.ritualsoftheold.terra.world.LoadMarker;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Handles loading of offheap worlds. Usually this class is used by load
 * markers; direct usage is not recommended for application developers.
 *
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
    private float centerX, centerY, centerZ;
    
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
     * Handles enlarging master octree when needed.
     */
    private WorldSizeManager sizeManager;
    
    /**
     * Initializes a new world loader.
     * @param octreeStorage Octree storage of the world.
     * @param chunkStorage Chunk storage of the world.
     * @param genManager World generation manager.
     * @param sizeManager World size manager.
     */
    public WorldLoader(OctreeStorage octreeStorage, ChunkStorage chunkStorage, WorldGenManager genManager, WorldSizeManager sizeManager) {
        this.octreeStorage = octreeStorage;
        this.chunkStorage = chunkStorage;
        this.genManager = genManager;
        this.sizeManager = sizeManager;
    }
    
    /**
     * Sets world center, master octree and world scale.
     * @param x Center X.
     * @param y Center Y.
     * @param z Center Z.
     * @param octree Master octree index.
     * @param scale Scale of the world.
     */
    public void worldConfig(float x, float y, float z, int octree, float scale) {
        this.centerX = x;
        this.centerY = y;
        this.centerZ = z;
        this.masterOctree = octree;
        this.worldScale = scale;
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
    public void seekArea(float x, float y, float z, float range, WorldLoadListener listener, boolean generate, LoadMarker trigger) {
        System.out.println("I WAS CALLED, scale: " + worldScale);
        /**
         * Node coordinates, start at world center.
         */
        float nodeX, nodeY, nodeZ;
        
        /**
         * Relative coordinates to current node. Starting from relative
         * to center of world.
         */
        float rX, rY, rZ;
        
        /**
         * Scale of node that is currently operated
         */
        float scale;
        
        // Enlarge world until the range fits in master octree
        while (true) {
            // Update/create some values we need later on
            nodeX = centerX;
            nodeY = centerY;
            nodeZ = centerZ;
            
            rX = x - centerX;
            rY = y - centerY;
            rZ = z - centerZ;
            
            scale = worldScale;
            System.out.println("worldscale: " + scale);
            
            System.out.println("relative: " + rX + ", " + rY + ", " + rZ);
            float subScale = 0.5f * scale;
            
            if (rX + range > nodeX + subScale || rX - range < nodeX - subScale // X coordinate
                || rY + range > nodeY + subScale || rY - range < nodeY - subScale // Y coordinate
                || rZ + range > nodeZ + subScale || rZ - range < nodeZ - subScale) { // Z coordinate
                checkEnlarge(nodeX, nodeY, nodeZ, rX, rY, rZ, scale, range);
                // Size manager will call us through OffheapWorld, updating our fields as needed
            } else {
                // No need to enlarge the world (anymore)
                break;
            }
        }
        
        /**
         * Id of current node, starts as master octree.
         */
        int nodeId = masterOctree;
        
        while (true) {
            System.out.println("LOOPY LOOP");
            // Figure out some stuff about current node
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
            float subNodeY = nodeY - posMod;
            float subNodeZ = nodeZ - posMod;
            // Increase coordinates now, then reduce them doubly if needed
            rX += posMod;
            rY += posMod;
            rZ += posMod;
            
            if (rX > 0) {
                index += 1;
                subNodeX += pos2Mod;
                rX -= pos2Mod;
            }
            if (rY > 0) {
                index += 2;
                subNodeY += pos2Mod;
                rY -= pos2Mod;
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
                    genManager.generate(addr, index, subNodeX, subNodeY, subNodeZ, scale);
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
                chunkStorage.ensureLoaded(node); // Use node content as chunk id and load it
                node = mem.readVolatileInt(nodeAddr);
                listener.chunkLoaded(chunkStorage.getTemporaryChunk(node, null), subNodeX, subNodeY, subNodeZ, trigger);
                break; // No further action necessary
            } else { // "Dereference" an octree
                nodeId = node; // New node id to content of current node
            }
            
            // Check if range is small enough to fit in that child node with our coordinates
            if (rX + range > subNodeX + posMod || rX - range < subNodeX - posMod // X coordinate
                    || rY + range > subNodeY + posMod || rY - range < subNodeY - posMod // Y coordinate
                    || rZ + range > subNodeZ + posMod || rZ - range < subNodeZ - posMod) { // Z coordinate
                // When ANY fails: we have found the node over which we must load all
                System.out.println("First call to loadArea with scale: " + scale + ", range: " + range);
                loadArea(x, y, z, range, nodeId, scale, nodeX, nodeY, nodeZ, listener, generate, trigger);
                break;
            }
            
            // If we are still going to continue, replace node coordinates with subnode coordinates
            nodeX = subNodeX;
            nodeY = subNodeY;
            nodeZ = subNodeZ;
            
            // And since this is not master octree anymore, remember to fire an event
            listener.octreeLoaded(addr, octreeStorage.getGroup(nodeId >>> 24), nodeId, subNodeX, subNodeY, subNodeZ, scale, trigger);
        }
    }
    
    private void checkEnlarge(float nodeX, float nodeY, float nodeZ, float rX, float rY, float rZ, float scale, float range) {
        /**
         * New world scale, if we enlarge master octree.
         */
        float newScale = scale * 2;
        float subScale = 0.5f * scale;
        
        /**
         * World size manager places the old master octree in this index
         * of new master octree. Start value anticipates that all coordinate axes
         * overflow towards positive values.
         */
        int oldIndex = 0;
        
        // If negative overflow happens, we priorize it
        if (rX - range < nodeX - subScale) {
            oldIndex += 1;
        } if (rY - range < nodeY - subScale) {
            oldIndex += 2;
        } if (rZ - range < nodeZ - subScale) {
            oldIndex += 4;
        }
        System.out.println("oldIndex: " + oldIndex);
        
        // Request size manager to enlarge the world
        sizeManager.enlarge(newScale, oldIndex);
    }
    
    /**
     * Loads given node and its children which are closer than given range
     * from the center of the area. Note that node center and area center
     * are probably different. If you do not have a node if and its center
     * coordinates, use {@link #seekArea(float, float, float, float, WorldLoadListener, boolean)}
     * instead; it will call this method once it has figured those out.
     * @param x X coordinate of center of area.
     * @param y Y coordinate of center of area.
     * @param z Z coordinate of center of area.
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
    public void loadArea(float x, float y, float z, float range, int nodeId, float scale,
            float nodeX, float nodeY, float nodeZ, WorldLoadListener listener, boolean generate, LoadMarker trigger) {
        //System.out.println("loadArea, scale: " + scale);
        // Fetch data about given node
        long addr = octreeStorage.getOctreeAddr(nodeId); // This also loads the octree with group it is in
        //System.out.println("loadArea addr: " + addr);
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
            //System.out.println("flag == " + flag);
            
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
                        // Check if this node is within range and does it, thus, need to be loaded
                        // This is done only for chunks, since octree structure must always be fully loaded
                        float subScale = 0.5f * scale;
                        if (subNodeX + subScale < x - range || subNodeX - subScale > x + range
                                || subNodeY + subScale < y - range || subNodeY - subScale > y + range
                                || subNodeZ + subScale < z - range|| subNodeZ - subScale > z + range) {
                            continue; // Apparently no, go straight to next one
                        }
                        
                        //System.out.println("Create chunk (i: " + i + ")");
                        genManager.generate(addr, i, subNodeX, subNodeY, subNodeZ, scale);
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
                    chunkStorage.ensureLoaded(node);
                    listener.chunkLoaded(chunkStorage.getTemporaryChunk(node, null), subNodeX, subNodeY, subNodeZ, trigger);
                } else { // Octree. Here comes recursion...
                    // TODO multithreading
                    //System.out.println("Octree path, node: " + node);
                    loadArea(x, y, z, range, node, scale, subNodeX, subNodeY, subNodeZ, listener, generate, trigger);
                }
            } // else: no action needed, single node was loaded with getOctreeAddr
            //System.out.println("end of i: " + i);
        }
    }
}
