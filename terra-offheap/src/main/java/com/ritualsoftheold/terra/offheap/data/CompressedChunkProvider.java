package com.ritualsoftheold.terra.offheap.data;

import com.ritualsoftheold.terra.offheap.chunk.ChunkStorage;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

// TODO blocked by chunk storage changes
public class CompressedChunkProvider implements WorldDataProvider {
    
    private static final Memory mem = OS.memory();
    
    private ChunkStorage storage;
    
    @Override
    public int newId() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void write(int id, int offset, short[] data) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void read(int id, int offset, short[] data) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isOctree() {
        // TODO Auto-generated method stub
        return false;
    }

}
