package com.ritualsoftheold.terra.offheap.data;

import com.ritualsoftheold.terra.offheap.chunk.compress.RLE22ChunkFormat;
import com.ritualsoftheold.terra.offheap.chunk.compress.UncompressedChunkFormat;
import com.ritualsoftheold.terra.offheap.octree.OctreeNodeFormat;

/**
 * Decides best storage option for given block data.
 *
 */
public class DataHeuristics {
    
    // Chunk data formats
    private WorldDataFormat rle22Chunk;
    private WorldDataFormat uncompressedChunk;
    
    // Octree data formats
    private WorldDataFormat octreeNode;
    
    public DataHeuristics() {
        this.rle22Chunk = new RLE22ChunkFormat();
        this.uncompressedChunk = new UncompressedChunkFormat();
        
        this.octreeNode = new OctreeNodeFormat();
    }
    
    /**
     * Gets (probably) best data format for data with given values.
     * @param matCount Material count. Required.
     * @return More or less suitable data provider for given data.
     */
    public WorldDataFormat getDataFormat(int matCount) {
        assert matCount >= 0;
        
        if (matCount == 1) {
            return octreeNode;
        } else {
            // TODO use RLE22, or other RLE when applicable
            // (at the moment, easier to debug just one type...)
            return uncompressedChunk;
        }
    }
}
