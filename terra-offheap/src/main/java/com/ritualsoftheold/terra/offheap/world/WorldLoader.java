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
    
    public void seekArea(float x, float y, float z, float range, WorldLoadListener listener, boolean generate) {
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
            int node = mem.readVolatileInt(addr + index * 4);
            int flag = flags >>> index & 1;
            
            // Check if additional loading is necessary...
            if (flag == 0) { // ... or if it is a single node after all
                break; // No further action necessary
            } else if (generate && node == 0) { // && flag == 1, naturally
                // Ok, this is something that has not been generated, so it is
                // "octree null": flag is 1, but node is 0 (kind of null pointer)
                // TODO
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
        // Fetch data about given node
        long addr = octreeStorage.getOctreeAddr(nodeId); // This also loads the octree with group it is in
        byte flags = mem.readVolatileByte(addr);
        
        // Fire event to listener
        listener.octreeLoaded(addr, octreeStorage.getGroup(nodeId >>> 24), nodeId, nodeX, nodeY, nodeZ, scale);
        
        // Operate with scale of child nodes
        scale *= 0.5f;
        float posMod = 0.5f * scale; // Parameter scale / 4
        
        // Loop through child nodes
        for (int i = 0; i < 8; i++) {
            int flag = flags >>> i & 1;
            
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
                
                int node = mem.readVolatileInt(addr + i * 4);
                if (scale == DataConstants.CHUNK_SCALE) { // It is a chunk!
                    // Load chunk and then fire event to listener
                    chunkStorage.ensureLoaded(node);
                    listener.chunkLoaded(chunkStorage.getTemporaryChunk(node, null), subNodeX, subNodeY, subNodeZ);
                } else { // Octree. Here comes recursion...
                    // TODO multithreading
                    loadArea(node, scale, subNodeX, subNodeY, subNodeZ, listener, generate);
                }
            } // else: no action needed, single node was loaded with getOctreeAddr
        }
    }
}
