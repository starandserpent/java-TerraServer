package com.ritualsoftheold.terra.server.manager.octree;

public interface UsageListener {

    void used(int id, float scale, float x, float y, float z);
    
    void unused(int id);
}
