package com.ritualsoftheold.terra.offheap.data;

/**
 * Decides best storage option for given block data.
 *
 */
public class DataHeuristics {
    
    // Octree-related providers
    private WorldDataProvider nodeProvider;
    private WorldDataProvider octreeProvider;
    
    // Chunk-related providers
    private WorldDataProvider compressedChunkProvider;
    
    /**
     * Gets (probably) best data provider for data with given values.
     * @param matCount Material count. Required.
     * @param isEightCubes If the data is eight cubes with same size, special
     * optimizations can usually be performed.
     * @return More or less suitable data provider for given data.
     */
    public WorldDataProvider getDataProvider(int matCount, boolean isEightCubes) {
        assert matCount > 0;
        
        if (matCount == 1) {
            return nodeProvider; // Octree single node provider
        } else if (isEightCubes) {
            assert false; // TODO
            assert matCount < 5;
            
            return octreeProvider; // Octree/8 nodes provider
        }
        
        return compressedChunkProvider;
    }
}
