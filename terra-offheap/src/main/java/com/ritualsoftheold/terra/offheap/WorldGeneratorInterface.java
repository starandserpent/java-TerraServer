package com.ritualsoftheold.terra.offheap;

import com.ritualsoftheold.terra.core.TerraModule;
import com.ritualsoftheold.terra.core.material.MaterialRegistry;
import com.ritualsoftheold.terra.offheap.chunk.ChunkLArray;

/**
 * Implementations of this generate the world from scratch.
 *
 */
public interface WorldGeneratorInterface {
    WorldGeneratorInterface setup(MaterialRegistry materialRegistry, TerraModule mod);
    void generate(ChunkLArray chunk);
}
