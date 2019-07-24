package com.ritualsoftheold.terra.offheap.world;

import com.ritualsoftheold.terra.core.material.MaterialRegistry;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.WorldGeneratorInterface;
import com.ritualsoftheold.terra.offheap.chunk.ChunkLArray;

/**
 * Handles loading of offheap worlds. Usually this class is used by load
 * markers; direct usage is not recommended for application developers.
 */
public class ChunkSVOGenerator {
    private float centerX;
    private float centerY;
    private float centerZ;

    private float genOriginX;
    private float genOriginY;
    private float genOriginZ;

    private int masterOctree;
    private float worldScale;
    private WorldGeneratorInterface generator;
    private MaterialRegistry reg;
    private int height;

    public ChunkSVOGenerator(WorldGeneratorInterface generator, MaterialRegistry reg, int height) {
        this.height = height;
        this.generator = generator;
        this.reg = reg;
        System.out.println("Called------------------------------- "+height);
    }

    public void seekSector(float x, float z, float range, WorldLoadListener listener, OffheapLoadMarker trigger) {

        this.centerX = x;
        this.centerZ = z;
        //TODO: temporary check, probably should make it a constant radius regardless of the height, width, depth

            for (float rangeY = -height * 2; rangeY <= height * 2; rangeY += 16) {
                loadArea(0, rangeY, 0, listener);
                for (float f = 1; f <= range; f++) {
                    for (float rangeZ = -f; rangeZ < f; rangeZ++) {
                        loadArea(-16 * rangeZ, rangeY, -16 * f, listener);
                        loadArea(16 * f, rangeY, 16 * rangeZ, listener);
                    }
                    for (float rangeX = -f; rangeX < f; rangeX++) {
                        loadArea(-16 * f, rangeY, -16 * rangeX, listener);
                        loadArea(16 * rangeX, rangeY, 16 * f, listener);
                    }
                    loadArea(16 * f, rangeY, 16 * f, listener);
                    loadArea(-16 * f, rangeY, -16 * f, listener);
                }
            }

    }
    public void seekSector(float x, float y, float z, float range, WorldLoadListener listener, OffheapLoadMarker trigger){
        this.centerX = x;
        this.centerY = y;
        this.centerZ = z;

        int chunkWorldSize = DataConstants.CHUNK_SCALE;

        this.genOriginX = this.centerX - (range*chunkWorldSize);
        this.genOriginY = this.centerY - (range*chunkWorldSize);
        this.genOriginZ = this.centerZ - (range*chunkWorldSize);
        System.out.println("Player loc: "+x+" "+y+" "+z);
        System.out.println("Origin: "+genOriginX+","+genOriginY+","+genOriginZ);
        int size = (int)(range)*2;
        int maxSize = size * size * size;
        for(int i = 0; i < maxSize; i++){
            int xOffset =  i % size;
            int yOffset =  (i/size)%size;
            int zOffset =  i/(size*size);

            float xWorld = (xOffset * chunkWorldSize) + genOriginX;
            float yWorld = (yOffset * chunkWorldSize) + genOriginY;
            float zWorld = (zOffset * chunkWorldSize) + genOriginZ;

            System.out.println("World coord: "+xWorld+" "+yWorld+" "+zWorld);

            loadArea(xWorld,yWorld,zWorld,listener);
        }

    }

    public void updateSector(float x, float z, float range, WorldLoadListener listener, OffheapLoadMarker trigger) {
        for(float rangeY = -height *2; rangeY <= height *2; rangeY +=16) {
            if (x > centerX) {
                for (float rangeZ = -range; rangeZ <= range; rangeZ++) {
                    loadArea(16 * range + x, rangeY, 16 * rangeZ + z, listener);
                }
                for (float rangeX = -range; rangeX <= range; rangeX++) {
                    //unloadArea(-16 * range + centerX, rangeY, -16 * rangeX + centerZ, listener, trigger);
                }
            } else if (x < centerX) {
                for (float rangeZ = -range; rangeZ <= range; rangeZ++) {
                    loadArea(-16 * range + x, rangeY, -16 * rangeZ + z, listener);
                }
                for (float rangeX = -range; rangeX <= range; rangeX++) {
                    //unloadArea(16 * range + centerX, rangeY, 16 * rangeX + centerZ, listener, trigger);
                }
            }

            if (z > centerZ) {
                for (float rangeZ = -range; rangeZ <= range; rangeZ++) {
                    //unloadArea(-16 * rangeZ + centerX, rangeY, -16 * range + centerZ, listener, trigger);
                }
                for (float rangeX = -range; rangeX <= range; rangeX++) {
                    loadArea(16 * rangeX + x, rangeY, 16 * range + z, listener);
                }
            } else if (z < centerZ) {
                for (float rangeX = -range; rangeX <= range; rangeX++) {
                    loadArea(-16 * rangeX + x, rangeY, -16 * range + z, listener);
                }
                for (float rangeZ = -range; rangeZ <= range; rangeZ++) {
                   // unloadArea(16 * rangeZ + centerX, rangeY, 16 * range + centerZ, listener, trigger);
                }
            }
        }
        centerX = x;
        centerZ = z;
    }

  /*  public void unloadArea(float x, float y, float z, WorldLoadListener listener, OffheapLoadMarker trigger){
        ChunkLoader chunkLoader = new ChunkLoader(listener);
        ChunkLArray chunk = chunkLoader.getChunk(x, y, z, trigger);
        if(chunk != null) {
            //genManager.remove(chunk);
            listener.chunkUnloaded(chunk);
        }
    }*/

    public void loadArea(float x, float y, float z, WorldLoadListener listener) {
        if(x > 0 && z > 0) {
            ChunkLArray chunk = new ChunkLArray(x, y, z, reg);
            generator.generate(chunk);
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
      /*  /**
         * Node coordinates, start at world center.
         */
       /*   float nodeX, nodeZ;
        
        /**
         * Relative coordinates to current node. Starting from relative
         * to center of world.

        float rX,  rZ;

        float scale;
        
        // Update/create some values we need later on
        nodeX = centerX;
        nodeZ = centerZ;
        
        rX = x - centerX;
        rZ = z - centerZ;
        
        scale = worldScale;
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
             //   listener.chunkLoaded(chunk);
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
        */
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
     */
    public void loadArea(float x, float z, float range, int nodeId, float scale){
          /*  float nodeX, float nodeY, float nodeZ, WorldLoadListener listener, boolean generate, OffheapLoadMarker trigger) {
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
                    //listener.chunkLoaded(chunk);
                 //   trigger.addBuffer(chunk.getChunkBuffer()); // Add to load marker
                } else { // Octree. Here comes recursion...
                    // TODO multithreading
                    //System.out.println("Octree path, node: " + node);
                    loadArea(x, z, range, node, scale, subNodeX, subNodeY, subNodeZ, listener, generate, trigger);
                }
            } // else: no action needed, single node was loaded with getOctreeAddr
            //System.out.println("end of i: " + i);
        }*/
    }
}
