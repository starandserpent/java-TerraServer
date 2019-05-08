package com.ritualsoftheold.terra.offheap.io;

import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.io.ChunkLoaderInterface;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;
import com.ritualsoftheold.terra.offheap.world.OffheapLoadMarker;
import com.ritualsoftheold.terra.offheap.world.WorldLoadListener;

public class ChunkLoader implements ChunkLoaderInterface {
    private WorldLoadListener loadListener;

    public ChunkLoader(WorldLoadListener loadListener) {
        this.loadListener = loadListener;
    }

    @Override
    public void loadChunk(float x, float y, float z, OffheapChunk chunk, OffheapLoadMarker marker) {
        loadListener.chunkLoaded(chunk, x, y, z, marker);
    }

    @Override
    public OffheapChunk getChunk(float x, float y, float z, OffheapLoadMarker loadMarker) {
        for (ChunkBuffer buffer:loadMarker.getBuffersInside()){
            for(int i = 0; i < buffer.getChunkCount(); i++){
                OffheapChunk chunk = buffer.getChunk(i);
                if(chunk.getX() == x && chunk.getY() == y && chunk.getZ() == z){
                    return chunk;
                }
            }
        }
        return null;
    }

    @Override
    public ChunkBuffer saveChunks(int index, ChunkBuffer buf) {
        return buf;
    }
}