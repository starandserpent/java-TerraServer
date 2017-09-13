package com.ritualsoftheold.terra.world.gen;

import com.ritualsoftheold.terra.material.MaterialRegistry;

/**
 * Implementations of this generate the world from scratch.
 *
 */
public interface WorldGenerator {
    
    public static class Metadata {
        
        /**
         * Contains amount of different materials in generated data.
         * This defaults to -1, which may cause Terra to calculate the
         * data. It is thus recommended to have a correct value set.
         */
        public int materialCount = -1;
        
    }
    
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
     * @param meta Metadata, which the generator can use to supply additional
     * information about generated data if desired. Additional data may improve
     * performance, but passing it is always optional.
     * @return If generating the chunk was successful.
     */
    boolean generate(short[] data, float x, float y, float z, float scale, Metadata meta);
}
