package com.ritualsoftheold.terra.offheap.data;

import com.ritualsoftheold.terra.offheap.node.OffheapChunk;

public interface CriticalBlockBuffer extends BufferWithFormat {

    OffheapChunk.Storage getStorage();
}
