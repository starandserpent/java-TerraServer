package com.ritualsoftheold.terra.manager.octree;

public interface UsageListener {

    void used(int id, float scale, float x, float y, float z);
    
    void unused(int id);
}
