package com.ritualsoftheold.terra.offheap.chunk;

import com.github.benmanes.caffeine.cache.Weigher;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;

public class ChunkWeighter implements Weigher<Integer, OffheapChunk> {

    @Override
    public int weigh(Integer key, OffheapChunk value) {
        return DataConstants.CHUNK_UNCOMPRESSED;
    }

}
