package com.ritualsoftheold.terra.offheap.world;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.world.LoadMarker;

public class OffheapLoadMarker extends LoadMarker {
    
    private static final VarHandle buffersVar = MethodHandles.arrayElementVarHandle(ChunkBuffer.class);
    private static final VarHandle bufCountVar;
    private static final VarHandle groupsVar = MethodHandles.arrayElementVarHandle(byte.class);
    private static final VarHandle groupCountVar;
    
    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            bufCountVar = lookup.findVarHandle(OffheapLoadMarker.class, "bufferCount", int.class);
            groupCountVar = lookup.findVarHandle(OffheapLoadMarker.class, "groupCount", int.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }
    }

    private volatile ChunkBuffer[] chunkBuffers;
    @SuppressWarnings("unused") // VarHandle
    private int bufferCount;
    
    private volatile byte[] octreeGroups;
    @SuppressWarnings("unused") // VarHandle
    private int groupCount;
    
    protected OffheapLoadMarker(float x, float y, float z, float hardRadius, float softRadius, int priority) {
        super(x, y, z, hardRadius, softRadius, priority);
    }
    
    public void addBuffer(ChunkBuffer buf) {
        buffersVar.setVolatile(this, bufCountVar.getAndAdd(this, 1), buf);
    }
    
    public void addGroup(byte id) {
        groupsVar.setVolatile(this, groupCountVar.getAndAdd(this, 1), id);
    }
    
    public ChunkBuffer[] getChunkBuffers() {
        return chunkBuffers;
    }
    
    public byte[] getOctreeGroups() {
        return octreeGroups;
    }
}
