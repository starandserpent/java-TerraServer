package com.ritualsoftheold.terra.offheap.world;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.ritualsoftheold.terra.offheap.octree.OctreeStorage;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Handles enlarging master octree, which is quite complex operation.
 *
 */
public class WorldSizeManager {
    
    private static final Memory mem = OS.memory();
    
    private OffheapWorld world;
    private OctreeStorage storage;
    
    private BlockingQueue<EnlargeRequest> queue;
    
    private static class EnlargeRequest {
        
        public EnlargeRequest(float scale, int oldIndex) {
            this.scale = scale;
            this.oldIndex = oldIndex;
        }
        
        private float scale;
        private int oldIndex;
    }
    
    private Thread handlerThread = new Thread(() -> {
        while (true) {
            try {
                EnlargeRequest request = queue.take(); // Wait on queue until requests come
                if (request.scale >= world.getMasterScale()) {
                    // Proceed only if we would actually be enlarging the master octree
                    enlarge(request.scale, request.oldIndex);
                }
            } catch (InterruptedException e) {
                // Oops
            }
        }
    });
    
    public WorldSizeManager(OffheapWorld world) {
        assert world != null;
        
        this.world = world;
        this.storage = world.getOctreeStorage();
        this.queue = new ArrayBlockingQueue<>(100, true);
        handlerThread.start();
    }
    
    public void enlarge(float scale, int oldIndex) {
        long stamp = world.enterExclusive();
        
        int oldId = storage.getMasterIndex();
        System.out.println("Old master octree: " + oldId);
        
        // Create new master octree
        int newId = oldId + 1;
        System.out.println("New master octree: " + newId);
        long newAddr = storage.getOctreeAddr(newId);
        
        // Get metadata addr for group 0 and update it
        long metaAddr = storage.getGroupMeta((byte) 0);
        mem.writeInt(metaAddr + 4, newId); // Id
        mem.writeFloat(metaAddr + 8, scale); // New scale
        
        // Calculate new center point (inverted lookup table from usual)
        float posMod = 0.25f * scale;
        float x = mem.readFloat(metaAddr + 12);
        float y = mem.readFloat(metaAddr + 16);
        float z = mem.readFloat(metaAddr + 20);
        switch (oldIndex) {
            case 0:
                x += posMod;
                y += posMod;
                z += posMod;
                break;
            case 1:
                x -= posMod;
                y += posMod;
                z += posMod;
                break;
            case 2:
                x += posMod;
                y -= posMod;
                z += posMod;
                break;
            case 3:
                x -= posMod;
                y -= posMod;
                z += posMod;
                break;
            case 4:
                x += posMod;
                y += posMod;
                z -= posMod;
                break;
            case 5:
                x -= posMod;
                y += posMod;
                z -= posMod;
                break;
            case 6:
                x += posMod;
                y -= posMod;
                z -= posMod;
                break;
            case 7:
                x -= posMod;
                y -= posMod;
                z -= posMod;
                break;
        }
        
        // Write new center coords
        mem.writeFloat(metaAddr + 12, x);
        mem.writeFloat(metaAddr + 16, y);
        mem.writeFloat(metaAddr + 20, z);
        
        // Place the old master octree at oldIndex pos in new octree
        mem.writeInt(newAddr + 1 + oldIndex * 4, oldId);
        
        // Tell world to switch to new master octree
        world.updateMasterOctree();
        
        world.leaveExclusive(stamp);
    }
    
    public void queueEnlarge(float scale, int oldIndex) {
        queue.add(new EnlargeRequest(scale, oldIndex));
    }
    
    // TODO shrinking world?
}
