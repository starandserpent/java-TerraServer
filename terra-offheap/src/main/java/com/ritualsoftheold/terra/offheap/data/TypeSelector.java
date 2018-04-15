package com.ritualsoftheold.terra.offheap.data;

import com.ritualsoftheold.terra.offheap.chunk.compress.Palette16ChunkFormat;
import com.ritualsoftheold.terra.offheap.chunk.compress.RLE22ChunkFormat;
import com.ritualsoftheold.terra.offheap.chunk.compress.UncompressedChunkFormat;
import com.ritualsoftheold.terra.offheap.octree.OctreeNodeFormat;

/**
 * Decides best storage option for given block data.
 *
 */
public class TypeSelector {
    
    // Chunk data formats
    private WorldDataFormat uncompressedChunk;
    private WorldDataFormat palette16Chunk;
    private WorldDataFormat rle22Chunk;
    
    // Octree data formats
    private WorldDataFormat octreeNode;
    
    public TypeSelector() {
        //this.rle22Chunk = RLE22ChunkFormat.INSTANCE;
        this.palette16Chunk = Palette16ChunkFormat.INSTANCE;
        this.uncompressedChunk = UncompressedChunkFormat.INSTANCE;
        
        this.octreeNode = new OctreeNodeFormat();
    }
    
    /**
     * Gets (probably) best data format for data with given values.
     * @param matCount Material count. Required.
     * @return More or less suitable data provider for given data.
     */
    public WorldDataFormat getDataFormat(int matCount) {
        if (matCount < 17) {
            return palette16Chunk;
        } else {
            return uncompressedChunk;
        }
    }
    
    public WorldDataFormat nextFormat(WorldDataFormat previous) {
        assert previous != null;
        
        if (previous == palette16Chunk) {
            return uncompressedChunk;
        }
        
        throw new IllegalArgumentException("next format not available");
    }
}
