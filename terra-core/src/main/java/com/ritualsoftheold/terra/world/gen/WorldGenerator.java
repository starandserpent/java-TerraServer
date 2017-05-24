package com.ritualsoftheold.terra.world.gen;

import com.ritualsoftheold.terra.material.MaterialRegistry;

/**
 * Implementations of this generate the world from scratch.
 *
 */
public interface WorldGenerator {
    
    /**
     * Initializes this world generator.
     * @param seed Seed value for generator.
     * @param materialRegistry Material registry to resolve material ids.
     * @return If this generator is now usable.
     */
    boolean initialize(long seed, MaterialRegistry materialRegistry);
    
    /**
     * Generates a part of world at given position.
     * @param data Data to fill. Each 0.25m space has one value. Use material ids.
     * @param x X coordinate of center of chunk.
     * @param y Y coordinate of center of chunk.
     * @param z Z coordinate of center of chunk.
     * @param scale Scale of the chunk.
     * @return If generating the chunk was successful.
     */
    boolean generate(short[] data, float x, float y, float z, float scale);
}
