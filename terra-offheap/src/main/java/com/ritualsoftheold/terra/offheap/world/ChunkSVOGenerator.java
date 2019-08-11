package com.ritualsoftheold.terra.offheap.world;

import com.ritualsoftheold.terra.core.material.MaterialRegistry;
import com.ritualsoftheold.terra.core.node.OctreeNode;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.WorldGeneratorInterface;
import com.ritualsoftheold.terra.offheap.chunk.ChunkLArray;
import com.ritualsoftheold.terra.offheap.node.OffheapOctree;
import com.ritualsoftheold.terra.offheap.util.Morton3D;

import java.sql.Timestamp;

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

    private float worldScale;
    private WorldGeneratorInterface generator;
    private MaterialRegistry reg;
    private int height;
    private Morton3D morton3D = new Morton3D();
    private OffheapOctree offheapOctree;

    ChunkSVOGenerator(WorldGeneratorInterface generator, MaterialRegistry reg, int height, OffheapOctree offheapOctree) {
        this.height = height;
        this.generator = generator;
        this.reg = reg;
        this.offheapOctree = offheapOctree;
        System.out.println("Called------------------------------- " + height);
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

  /*  void seekSector(float x, float y, float z, float range, WorldLoadListener listener, OffheapLoadMarker trigger) {
        this.centerX = x;
        this.centerY = y;
        this.centerZ = z;

        int chunkWorldSize = DataConstants.CHUNK_SCALE;

        this.genOriginX = this.centerX - (range * chunkWorldSize);
        this.genOriginY = this.centerY - (range * chunkWorldSize);
        this.genOriginZ = this.centerZ - (range * chunkWorldSize);
        System.out.println("Player loc: " + x + " " + y + " " + z);
        System.out.println("Origin: " + genOriginX + "," + genOriginY + "," + genOriginZ);
        int size = (int) (range) * 2;
        int maxSize = size * size * size;
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        // System.out.println("Started sector seek "+timestamp);

        OctreeNode[] OctreeLeafs = new OctreeNode[maxSize];

        for (int i = 0; i < maxSize; i++) {
            int xOffset = i % size;
            int yOffset = (i / size) % size;
            int zOffset = i / (size * size);

            float xWorld = (xOffset * chunkWorldSize) + genOriginX;
            float yWorld = (yOffset * chunkWorldSize) + genOriginY;
            float zWorld = (zOffset * chunkWorldSize) + genOriginZ;

//            System.out.println("World coord: "+xWorld+" "+yWorld+" "+zWorld);
            long lolong = morton3D.encode(xOffset, yOffset, zOffset);
            //System.out.println(lolong);
//            loadArea(xWorld,yWorld,zWorld,listener);
            OctreeNode leafNode = new OctreeNode();
            leafNode.locCode = lolong;
            OctreeLeafs[(int) lolong] = leafNode;

        }
        offheapOctree.SetOctreeOrigin((int) x, (int) y, (int) z, maxSize);
        //  offheapOctree.createOctree(OctreeLeafs);
        //timestamp = new Timestamp(System.currentTimeMillis());
        //System.out.println("Ended sector seek "+timestamp);
    }*/

    public void updateSector(float x, float z, float range, WorldLoadListener listener, OffheapLoadMarker trigger) {
        for (float rangeY = -height * 2; rangeY <= height * 2; rangeY += 16) {
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
        if (x > 0 && z > 0) {
            ChunkLArray chunk = new ChunkLArray(x, y, z, reg);
            generator.generate(chunk);
            listener.chunkLoaded(chunk);
        }
    }
}
