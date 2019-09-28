package com.ritualsoftheold.terra.server.chunks;

import com.ritualsoftheold.terra.core.chunk.ChunkLArray;
import com.ritualsoftheold.terra.core.materials.Registry;
import com.ritualsoftheold.terra.core.materials.TerraModule;

public interface ChunkGenerator {
    ChunkLArray getChunk(float posX, float posY, float posZ);

    void setMaterials(TerraModule mod, Registry reg);
}
