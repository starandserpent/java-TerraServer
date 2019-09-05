package com.ritualsoftheold.terra.server.manager.gen.interfaces;

import com.ritualsoftheold.terra.core.chunk.ChunkLArray;
import com.ritualsoftheold.terra.core.materials.Registry;
import com.ritualsoftheold.terra.core.materials.TerraModule;

/**
 * Implementations of this generate the world from scratch.
 */
public interface WorldGeneratorInterface {
    WorldGeneratorInterface setup(Registry registry, TerraModule mod);
    void generate(ChunkLArray chunk);
}
