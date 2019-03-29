package com.ritualsoftheold.terra.gen.tasks;

/**
 * Represents a task of world generation. Contains data that does not change
 * during the generation.
 *
 */
public class GenerationTask {

    private final float x, y, z;
    
    public GenerationTask(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
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
