package com.ritualsoftheold.terra.manager;

import com.ritualsoftheold.terra.manager.material.TerraModule;
import com.ritualsoftheold.terra.manager.material.Registry;
import com.ritualsoftheold.terra.memory.chunk.ChunkLArray;

/**
 * Implementations of this generate the world from scratch.
 *
 */
public interface WorldGeneratorInterface {
    WorldGeneratorInterface setup(Registry registry, TerraModule mod);
    void generate(ChunkLArray chunk);
}
