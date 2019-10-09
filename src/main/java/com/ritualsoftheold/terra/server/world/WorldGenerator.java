package com.ritualsoftheold.terra.server.world;

import com.ritualsoftheold.terra.core.DataConstants;
import com.ritualsoftheold.terra.core.WorldLoadListener;
import com.ritualsoftheold.terra.core.chunk.ChunkLArray;
import com.ritualsoftheold.terra.core.octrees.OctreeLeaf;
import com.ritualsoftheold.terra.core.octrees.OctreeNode;
import com.ritualsoftheold.terra.core.utils.CoreUtils;
import com.ritualsoftheold.terra.server.LoadMarker;
import com.ritualsoftheold.terra.core.octrees.OffheapOctree;
import com.ritualsoftheold.terra.core.utils.Morton3D;
import org.apache.commons.collections4.map.MultiKeyMap;

/**
 * Handles loading of offheap worlds. Usually this class is used by load
 * markers; direct usage is not recommended for application developers.
 */
class WorldGenerator {

    private final OffheapOctree octree;
    private ChunkGenerator generator;

    //This is recommend max static octree size because it takes 134 MB
    private static final int MAX_LEAF_SIZE = 16777216;
    private static final int MAX_OCTANT_LAYERS = 4;

    private MultiKeyMap<Integer, OctreeLeaf> chunkMap;

    private final int nodeLength;
    private final int maxOctreeSize;

    WorldGenerator(int nodeLength, int maxOctreeSize, OffheapOctree octree, ChunkGenerator generator) {
        this.generator = generator;
        this.octree = octree;
        this.nodeLength = nodeLength;
        this.maxOctreeSize = maxOctreeSize;

        chunkMap = new MultiKeyMap<>();
    }

    //Initial generation
    void seekSector(LoadMarker marker) {

        //Round world size to nearest node length
        marker.calculateMarkerOctants(nodeLength);

        int layers = CoreUtils.calculateOctreeLayers(nodeLength);

        int roundRadius = (int) (2 * marker.getHardRadius()) - 1;

        if (roundRadius > ServerWorld.MAX_LOAD_DISTANCE / 16) {
            roundRadius = ServerWorld.MAX_LOAD_DISTANCE / 16;
        }

        int chunkAmount = (int) Math.pow(roundRadius, 3);

        if (nodeLength > ServerWorld.MAX_LOAD_DISTANCE) {
            for (int l = 0; l < layers - MAX_OCTANT_LAYERS; l++) {
                int octant = marker.getOctant(l);
                octree.createNextLayer(octant);
            }
        }

        for (int i = 0; i < chunkAmount; i++) {
            int xOffset = i % roundRadius;
            int yOffset = (i / roundRadius) % roundRadius;
            int zOffset = i / (roundRadius * roundRadius);

            int xWorld = (int) ((xOffset * DataConstants.CHUNK_SCALE)
                    + (marker.getPosX() / DataConstants.CHUNK_SCALE) * DataConstants.CHUNK_SCALE);
            int yWorld = (int) ((yOffset * DataConstants.CHUNK_SCALE)
                    + (marker.getPosY() / DataConstants.CHUNK_SCALE) * DataConstants.CHUNK_SCALE);
            int zWorld = (int) ((zOffset * DataConstants.CHUNK_SCALE)
                    + (marker.getPosZ() / DataConstants.CHUNK_SCALE) * DataConstants.CHUNK_SCALE);

            long lolong = Morton3D.encode(xOffset, yOffset, zOffset);

            ChunkLArray chunk = loadArea(xWorld, yWorld, zWorld, marker);

            OctreeLeaf leafNode = new OctreeLeaf(xWorld, yWorld, zWorld, layers, lolong, chunk);

            chunkMap.put(xWorld, yWorld, zWorld, leafNode);
        }

        createOctree(octree.getCursorNode());
        marker.sendOctree(octree);
        chunkMap.clear();
    }

    //Procedural generation
    void updateSector(float x, float z, float range, WorldLoadListener listener, LoadMarker trigger) {
    }

    private void createOctree(OctreeNode mainNode) {
        if (mainNode.layer <= octree.octreeLayers + 1) {

            OctreeNode child = CoreUtils.createNode(mainNode, 0);
            mainNode.setChildren(child, 0);
            assert child != null;
            octree.addOctant(child);
            createOctree(child);

            child = CoreUtils.createNode(mainNode, 1);
            mainNode.setChildren(child, 1);
            assert child != null;
            octree.addOctant(child);
            createOctree(child);

            child = CoreUtils.createNode(mainNode, 2);
            mainNode.setChildren(child, 2);
            assert child != null;
            octree.addOctant(child);
            createOctree(child);

            child = CoreUtils.createNode(mainNode, 3);
            mainNode.setChildren(child, 3);
            assert child != null;
            octree.addOctant(child);
            createOctree(child);

            child = CoreUtils.createNode(mainNode, 4);
            mainNode.setChildren(child, 4);
            assert child != null;
            octree.addOctant(child);
            createOctree(child);

            child = CoreUtils.createNode(mainNode, 5);
            mainNode.setChildren(child, 5);
            assert child != null;
            octree.addOctant(child);
            createOctree(child);

            child = CoreUtils.createNode(mainNode, 6);
            mainNode.setChildren(child, 6);
            assert child != null;
            octree.addOctant(child);
            createOctree(child);

            child = CoreUtils.createNode(mainNode, 7);
            mainNode.setChildren(child, 7);
            assert child != null;
            octree.addOctant(child);
            createOctree(child);
        } else {
            //1. Cube
            int posX = (int) mainNode.getPosX() + DataConstants.CHUNK_SCALE;
            int posY = (int) mainNode.getPosY() + DataConstants.CHUNK_SCALE;
            int posZ = (int) mainNode.getPosZ() + DataConstants.CHUNK_SCALE;

            OctreeNode child = chunkMap.get(posX, posY, posZ);
            if (child == null) {
                child = new OctreeLeaf(posX, posY, posZ, mainNode.layer + 1);
            }
            octree.addOctant(child);
            mainNode.setChildren(child, 0);

            //2. Cube
            posX = (int) mainNode.getPosX();
            posY = (int) mainNode.getPosY() + DataConstants.CHUNK_SCALE;
            posZ = (int) mainNode.getPosZ() + DataConstants.CHUNK_SCALE;

            child = chunkMap.get(posX, posY, posZ);
            if (child == null) {
                child = new OctreeLeaf(posX, posY, posZ, mainNode.layer + 1);
            }
            octree.addOctant(child);
            mainNode.setChildren(child, 1);

            //3. Cube
            posX = (int) mainNode.getPosX() + DataConstants.CHUNK_SCALE;
            posY = (int) mainNode.getPosY();
            posZ = (int) mainNode.getPosZ() + DataConstants.CHUNK_SCALE;

            child = chunkMap.get(posX, posY, posZ);
            if (child == null) {
                child = new OctreeLeaf(posX, posY, posZ, mainNode.layer + 1);
            }
            octree.addOctant(child);
            mainNode.setChildren(child, 2);

            //4. Cube
            posX = (int) mainNode.getPosX();
            posY = (int) mainNode.getPosY();
            posZ = (int) mainNode.getPosZ() + DataConstants.CHUNK_SCALE;

            child = chunkMap.get(posX, posY, posZ);
            if (child == null) {
                child = new OctreeLeaf(posX, posY, posZ, mainNode.layer + 1);
            }
            octree.addOctant(child);
            mainNode.setChildren(child, 3);

            //5. Cube
            posX = (int) mainNode.getPosX() + DataConstants.CHUNK_SCALE;
            posY = (int) mainNode.getPosY() + DataConstants.CHUNK_SCALE;
            posZ = (int) mainNode.getPosZ();

            child = chunkMap.get(posX, posY, posZ);
            if (child == null) {
                child = new OctreeLeaf(posX, posY, posZ, mainNode.layer + 1);
            }
            octree.addOctant(child);
            mainNode.setChildren(child, 4);

            //6. Cube
            posX = (int) mainNode.getPosX();
            posY = (int) mainNode.getPosY() + DataConstants.CHUNK_SCALE;
            posZ = (int) mainNode.getPosZ();

            child = chunkMap.get(posX, posY, posZ);
            if (child == null) {
                child = new OctreeLeaf(posX, posY, posZ, mainNode.layer + 1);
            }
            octree.addOctant(child);
            mainNode.setChildren(child, 5);

            //7. Cube
            posX = (int) mainNode.getPosX() + DataConstants.CHUNK_SCALE;
            posY = (int) mainNode.getPosY();
            posZ = (int) mainNode.getPosZ();

            child = chunkMap.get(posX, posY, posZ);
            if (child == null) {
                child = new OctreeLeaf(posX, posY, posZ, mainNode.layer + 1);
            }
            octree.addOctant(child);
            mainNode.setChildren(child, 6);

            //8. Cube
            posX = (int) mainNode.getPosX();
            posY = (int) mainNode.getPosY();
            posZ = (int) mainNode.getPosZ();

            child = chunkMap.get(posX, posY, posZ);
            if (child == null) {
                child = new OctreeLeaf(posX, posY, posZ, mainNode.layer + 1);
            }
            octree.addOctant(child);
            mainNode.setChildren(child, 7);
        }
    }

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
    private ChunkLArray loadArea(float x, float y, float z, LoadMarker marker) {
        if (x >= 0 && z >= 0) {
            ChunkLArray chunk = generator.getChunk(x, y, z);
            marker.sendChunk(chunk);
            return chunk;
        }

        return null;
    }
}
