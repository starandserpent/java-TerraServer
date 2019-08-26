package com.ritualsoftheold.terra.manager.data;

import com.ritualsoftheold.terra.manager.node.OffheapChunk;

public interface CriticalBlockBuffer extends BufferWithFormat {

    OffheapChunk.Storage getStorage();
}
