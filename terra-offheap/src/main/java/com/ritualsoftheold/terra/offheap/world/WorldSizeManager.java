package com.ritualsoftheold.terra.offheap.world;

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
    
    public WorldSizeManager(OffheapWorld world) {
        assert world != null;
        
        this.world = world;
        this.storage = world.getOctreeStorage();
    }
    
    public synchronized void enlarge(float scale, int oldIndex) {
        // Get metadata address for group 0 (where all master octrees originate)
        long metaAddr = storage.getGroupMeta(0);
        float oldScale = mem.readVolatileFloat(metaAddr + 8);
        
        // This method is synchronized, so only one thread at time can execute it
        // Thus, we can simply check if request we got is still valid...
        if (oldScale > scale) {
            return; // No need to do anything
            // Someone else enlarged the world AND updated world loader's data
        }
        
        int oldId = storage.getMasterIndex();
        System.out.println("Old master octree: " + oldId);
        
        // Create new master octree
        int newId = oldId + 1;
        System.out.println("New master octree: " + newId);
        long newAddr = storage.getOctreeAddr(newId);
        
        // Calculate new center point (inverted lookup table from usual)
        float posMod = 0.25f * scale;
        float x = mem.readVolatileFloat(metaAddr + 12);
        float y = mem.readVolatileFloat(metaAddr + 16);
        float z = mem.readVolatileFloat(metaAddr + 20);
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
        mem.writeVolatileFloat(metaAddr + 12, x);
        mem.writeVolatileFloat(metaAddr + 16, y);
        mem.writeVolatileFloat(metaAddr + 20, z);
        
        // Place the old master octree at oldIndex pos in new octree
        mem.writeVolatileInt(newAddr + 1 + oldIndex * 4, oldId);
        
        // When new octree is set up, write it to replace old one
        mem.writeVolatileInt(metaAddr + 4, newId); // Id
        mem.writeVolatileFloat(metaAddr + 8, scale); // New scale
        
        // Tell world to switch to new master octree
        world.updateMasterOctree();
    }
    
    // TODO shrinking world?
}
