package com.ritualsoftheold.terra.manager.io;

import com.ritualsoftheold.terra.manager.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.manager.node.OffheapChunk;
import com.ritualsoftheold.terra.manager.world.OffheapLoadMarker;
import com.ritualsoftheold.terra.manager.world.WorldLoadListener;

public class ChunkLoader implements ChunkLoaderInterface {
    private WorldLoadListener loadListener;

    public ChunkLoader(WorldLoadListener loadListener) {
        this.loadListener = loadListener;
    }

    @Override
    public void loadChunk(OffheapChunk chunk) {
        /*loadListener.chunkLoaded(chunk);*/
    }

    @Override
    public synchronized OffheapChunk getChunk(float x, float y, float z, OffheapLoadMarker loadMarker) {
    /*    for (ChunkBuffer buffer:loadMarker.getBuffersInside()){
            for(int i = 0; i < buffer.getChunkCount(); i++){
                ChunkLArray chunk = buffer.getChunk(i);
                if(chunk.getX() == x && chunk.getY() == y && chunk.getZ() == z){
                    return chunk;
                }
            }
        }*/
        return null;
    }

    @Override
    public ChunkBuffer saveChunks(int index, ChunkBuffer buf) {
        return buf;
    }
}