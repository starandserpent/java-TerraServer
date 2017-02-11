package com.ritualsoftheold.terra.files;

import com.ritualsoftheold.terra.node.Chunk;
import com.ritualsoftheold.terra.node.Octree;
import com.ritualsoftheold.terra.world.DataProvider;

/**
 * File-based data provider for Terra's worlds.
 *
 */
public class FileDataProvider implements DataProvider {

    @Override
    public Octree masterOctree() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Octree octree(long index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Chunk chunk(long index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void l_getOctree(long[] data, int uint_index) throws UnsupportedOperationException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public long l_getChunkPtr(int uint_index) throws UnsupportedOperationException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void l_getChunk(long[] data, long ptr) throws UnsupportedOperationException {
        // TODO Auto-generated method stub
        
    }

}
