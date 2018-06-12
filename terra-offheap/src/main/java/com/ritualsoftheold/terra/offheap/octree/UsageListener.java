package com.ritualsoftheold.terra.offheap.octree;

public interface UsageListener {

    void used(int id);
    
    void unused(int id);
}
