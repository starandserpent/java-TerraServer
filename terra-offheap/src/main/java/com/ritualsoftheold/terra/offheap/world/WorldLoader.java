package com.ritualsoftheold.terra.offheap.world;

import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.ChunkStorage;
import com.ritualsoftheold.terra.offheap.octree.OctreeStorage;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Handles loading parts of offheap world.
 *
 */
public class WorldLoader {
    
    private static final Memory mem = OS.memory();
    
    private OctreeStorage octreeStorage;
    
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
    
    public WorldLoader(OctreeStorage octreeStorage, ChunkStorage chunkStorage, WorldGenManager genManager) {
        this.octreeStorage = octreeStorage;
        this.chunkStorage = chunkStorage;
        this.genManager = genManager;
    }
    
    public void worldConfig(float x, float y, float z, int octree, float scale) {
        this.centerX = x;
        this.centerY = y;
        this.centerZ = z;
        this.masterOctree = octree;
        this.worldScale = scale;
    }
    
    public void seekArea(float x, float y, float z, float range, WorldLoadListener listener, boolean generate) {
        System.out.println("I WAS CALLED");
        /**
         * Node coordinates, start at world center.
         */
        float nodeX = centerX, nodeY = centerY, nodeZ = centerZ;
        
        /**
         * Relative coordinates to current node. Starting from relative
         * to center of world.
         */
        float rX = x - centerX, rY = y - centerY, rZ = z - centerZ;
        
        /**
         * Scale of node that is currently operated
         */
        float scale = worldScale;
        
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
                listener.chunkLoaded(chunkStorage.getTemporaryChunk(node, null), subNodeX, subNodeY, subNodeZ);
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
                loadArea(nodeId, scale, nodeX, nodeY, nodeZ, listener, generate);
                break;
            }
            
            // If we are still going to continue, replace node coordinates with subnode coordinates
            nodeX = subNodeX;
            nodeY = subNodeY;
            nodeZ = subNodeZ;
            
            // And since this is not master octree anymore, remember to fire an event
            listener.octreeLoaded(addr, octreeStorage.getGroup(nodeId >>> 24), nodeId, subNodeX, subNodeY, subNodeZ, scale);
        }
    }
    
    public void loadArea(int nodeId, float scale, float nodeX, float nodeY, float nodeZ, WorldLoadListener listener, boolean generate) {
        //System.out.println("loadArea, scale: " + scale);
        // Fetch data about given node
        long addr = octreeStorage.getOctreeAddr(nodeId); // This also loads the octree with group it is in
        //System.out.println("loadArea addr: " + addr);
        byte flags = mem.readVolatileByte(addr);
        addr += 1; // Skip the flags to data
        
        // Fire event to listener
        listener.octreeLoaded(addr, octreeStorage.getGroup(nodeId >>> 24), nodeId, nodeX, nodeY, nodeZ, scale);
        
        // Operate with scale of child nodes
        scale *= 0.5f;
        float posMod = 0.5f * scale; // Parameter scale / 4
        
        // Loop through child nodes
        for (int i = 0; i < 8; i++) {
            int flag = flags >>> i & 1;
            //System.out.println("flag == " + flag);
            
            // Octree or chunk
            if (flag == 1) {
                long nodeAddr = addr + i * 4;
                int node = mem.readVolatileInt(nodeAddr);
                
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
                
                // Check if node exists, and create if it doesn't
                if (node == 0) {
                    if (!generate) { // Generation is disallowed
                        continue;
                    }
                    
                    // Ok, this is something that has not been generated, so it is
                    // "octree null": flag is 1, but node is 0 (kind of null pointer)
                    if (scale == DataConstants.CHUNK_SCALE) {
                        //System.out.println("Create chunk (i: " + i + ")");
                        genManager.generate(addr, i, subNodeX, subNodeY, subNodeZ, scale);
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
                    listener.chunkLoaded(chunkStorage.getTemporaryChunk(node, null), subNodeX, subNodeY, subNodeZ);
                } else { // Octree. Here comes recursion...
                    // TODO multithreading
                    //System.out.println("Octree path, node: " + node);
                    loadArea(node, scale, subNodeX, subNodeY, subNodeZ, listener, generate);
                }
            } // else: no action needed, single node was loaded with getOctreeAddr
            //System.out.println("end of i: " + i);
        }
    }
}
