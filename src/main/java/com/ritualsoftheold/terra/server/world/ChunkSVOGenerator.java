package com.ritualsoftheold.terra.server.world;

import com.ritualsoftheold.terra.core.DataConstants;
import com.ritualsoftheold.terra.core.WorldLoadListener;
import com.ritualsoftheold.terra.core.chunk.ChunkLArray;
import com.ritualsoftheold.terra.core.octrees.OctreeBase;
import com.ritualsoftheold.terra.core.octrees.OctreeLeaf;
import com.ritualsoftheold.terra.server.LoadMarker;
import com.ritualsoftheold.terra.server.chunks.ChunkGenerator;
import com.ritualsoftheold.terra.server.morton.Morton3D;
import com.ritualsoftheold.terra.server.chunks.OffheapOctree;

import java.util.ArrayList;

/**
 * Handles loading of offheap worlds. Usually this class is used by load
 * markers; direct usage is not recommended for application developers.
 */
class ChunkSVOGenerator {

    private Morton3D morton3D = new Morton3D();
    private OffheapOctree masterOctree;
    private ChunkGenerator generator;
    private ArrayList<OctreeBase> nodes;

    private final int centerX;
    private final int centerY;
    private final int centerZ;

    private final int worldSize;

    ChunkSVOGenerator(int centerX, int centerY, int centerZ, int worldSize, ChunkGenerator generator) {
        this.generator = generator;
        nodes = new ArrayList<>(worldSize);
        masterOctree = new OffheapOctree();

        this.centerX = (centerX / 16) * 16;
        this.centerY = (((centerY - (worldSize * DataConstants.CHUNK_SCALE)) / 2) / 16) * 16;
        this.centerZ = (centerZ / 16) * 16;
        this.worldSize = worldSize;

        System.out.println("Planet octree center: " + this.centerX + "," + this.centerY + "," + this.centerZ);
    }

    //Initial generation
    void seekSector(LoadMarker marker) {

        float x = marker.getPosX();
        float y = marker.getPosY();
        float z = marker.getPosZ();

        System.out.println("Player loc: " + x + " " + y + " " + z);
        int maxSize = worldSize * worldSize * worldSize;

        OctreeBase[] octreeLeafs = new OctreeBase[maxSize];

        for (int i = 0; i < maxSize; i++) {
            int xOffset = i % worldSize;
            int yOffset = (i / worldSize) % worldSize;
            int zOffset = i / (worldSize * worldSize);

            float xWorld = (xOffset * DataConstants.CHUNK_SCALE) + this.centerX;
            float yWorld = (yOffset * DataConstants.CHUNK_SCALE) + this.centerY;
            float zWorld = (zOffset * DataConstants.CHUNK_SCALE) + this.centerZ;

            long lolong = morton3D.encode(xOffset, yOffset, zOffset);

            loadArea(xWorld, yWorld, zWorld, marker);

            OctreeLeaf leafNode = new OctreeLeaf();
            leafNode.worldX = xWorld;
            leafNode.worldY = yWorld;
            leafNode.worldZ = zWorld;
            leafNode.locCode = lolong;
            octreeLeafs[(int) lolong] = leafNode;
        }

        masterOctree.setOctreeOrigin((int) x, (int) y, (int) z, maxSize);
        masterOctree.createOctree(octreeLeafs);
        if (nodes != null) {
            nodes.addAll(masterOctree.getOctreeNodes());
        }
    }

    //Procedural generation
    void updateSector(float x, float z, float range, WorldLoadListener listener, LoadMarker trigger) {}

    //Unloads chunks
  /*  public void unloadArea(float x, float y, float z, WorldLoadListener listener, OffheapLoadMarker trigger){
        ChunkLoader chunkLoader = new ChunkLoader(listener);
        ChunkLArray chunk = chunkLoader.getChunk(x, y, z, trigger);
        if(chunk != null) {
            //genManager.remove(chunk);
            listener.chunkUnloaded(chunk);
        }
    }*/

    //Loads chunks
    private void loadArea(float x, float y, float z, LoadMarker marker) {
        if (x > 0 && z > 0) {
            ChunkLArray chunk = generator.getChunk(x, y, z);
            marker.sendChunk(chunk);
        }
    }
}
