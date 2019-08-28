package com.ritualsoftheold.terra.server.manager.gen.tasks;

/**
 * Represents a task of world generation. Contains data that does not change
 * during the generation.
 *
 */
public class GenerationTask {

    private float x, y, z;
    
    public GenerationTask(float x, float y, float z) {
        this.x = x;
        this.z = z;
        this.y = y;
    }

    public float getX() {
        return x;
    }
    
    public float getY() {
        return y;
    }
    
    public float getZ() {
        return z;
    }
}
