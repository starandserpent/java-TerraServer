package com.ritualsoftheold.terra.offheap.world;

import com.ritualsoftheold.terra.offheap.material.Registry;
import com.ritualsoftheold.terra.offheap.node.OctreeBase;
import com.ritualsoftheold.terra.offheap.node.OctreeLeaf;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.WorldGeneratorInterface;
import com.ritualsoftheold.terra.offheap.chunk.ChunkLArray;
import com.ritualsoftheold.terra.offheap.node.OffheapOctree;
import com.ritualsoftheold.terra.offheap.util.Morton3D;

import java.util.ArrayList;

/**
 * Handles loading of offheap worlds. Usually this class is used by load
 * markers; direct usage is not recommended for application developers.
 */
class ChunkSVOGenerator {

    private WorldGeneratorInterface generator;
    private Registry reg;
    private Morton3D morton3D = new Morton3D();
    private OffheapOctree offheapOctree;

    ChunkSVOGenerator(WorldGeneratorInterface generator, Registry reg, int height, OffheapOctree offheapOctree) {
        this.generator = generator;
        this.reg = reg;
        this.offheapOctree = offheapOctree;
        System.out.println("Called------------------------------- " + height);
    }

    void seekSector(float x, float y, float z, float range, WorldLoadListener listener, ArrayList<OctreeBase> nodes) {

        int chunkWorldSize = DataConstants.CHUNK_SCALE;

        float genOriginX = x - (range * chunkWorldSize);
        float genOriginY = y - (range * chunkWorldSize);
        float genOriginZ = z - (range * chunkWorldSize);
        System.out.println("Player loc: " + x + " " + y + " " + z);
        System.out.println("Origin: " + genOriginX + "," + genOriginY + "," + genOriginZ);
        int size = (int) (range) * 2;
        int maxSize = size * size * size;

        OctreeBase[] octreeLeafs = new OctreeBase[maxSize];

        for (int i = 0; i < maxSize; i++) {
            int xOffset = i % size;
            int yOffset = (i / size) % size;
            int zOffset = i / (size * size);

            float xWorld = (xOffset * chunkWorldSize) + genOriginX;
            float yWorld = (yOffset * chunkWorldSize) + genOriginY;
            float zWorld = (zOffset * chunkWorldSize) + genOriginZ;

            long lolong = morton3D.encode(xOffset, yOffset, zOffset);

            loadArea(xWorld, yWorld, zWorld, listener);

            OctreeLeaf leafNode = new OctreeLeaf();
            leafNode.worldX = xWorld;
            leafNode.worldY = yWorld;
            leafNode.worldZ = zWorld;
            leafNode.locCode = lolong;
            octreeLeafs[(int) lolong] = leafNode;
        }

        offheapOctree.SetOctreeOrigin((int) x, (int) y, (int) z, maxSize);
        offheapOctree.createOctree(octreeLeafs);
        if (nodes != null) {
            nodes.addAll(offheapOctree.getOctreeNodes());
        }
    }

    void updateSector(float x, float z, float range, WorldLoadListener listener, OffheapLoadMarker trigger) {

    }

  /*  public void unloadArea(float x, float y, float z, WorldLoadListener listener, OffheapLoadMarker trigger){
        ChunkLoader chunkLoader = new ChunkLoader(listener);
        ChunkLArray chunk = chunkLoader.getChunk(x, y, z, trigger);
        if(chunk != null) {
            //genManager.remove(chunk);
            listener.chunkUnloaded(chunk);
        }
    }*/

    private void loadArea(float x, float y, float z, WorldLoadListener listener) {
        if (x > 0 && z > 0) {
            ChunkLArray chunk = new ChunkLArray(x, y, z, reg);
            generator.generate(chunk);
            listener.chunkLoaded(chunk);
        }
    }
}
