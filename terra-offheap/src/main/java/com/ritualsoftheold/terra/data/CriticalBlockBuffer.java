package com.ritualsoftheold.terra.data;

import com.ritualsoftheold.terra.node.OffheapChunk;

public interface CriticalBlockBuffer extends BufferWithFormat {

    OffheapChunk.Storage getStorage();
}
