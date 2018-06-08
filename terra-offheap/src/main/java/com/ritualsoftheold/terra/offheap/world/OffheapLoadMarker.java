package com.ritualsoftheold.terra.offheap.world;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.world.LoadMarker;

public class OffheapLoadMarker extends LoadMarker {
    
    private static final VarHandle groupsVar = MethodHandles.arrayElementVarHandle(boolean[].class);
    
    private final ConcurrentMap<Integer, ChunkBuffer> chunkBuffers;
    
    private volatile boolean[] octreeGroups;
    
    protected OffheapLoadMarker(float x, float y, float z, float hardRadius, float softRadius, int priority) {
        super(x, y, z, hardRadius, softRadius, priority);
        this.chunkBuffers = new ConcurrentHashMap<>();
        this.octreeGroups = new boolean[256];
    }
    
    public void addBuffer(ChunkBuffer buf) {
        chunkBuffers.put(buf.getId(), buf);
    }
    
    public void addGroup(int id) {
        groupsVar.setVolatile(octreeGroups, id, true);
    }
    
    public Collection<ChunkBuffer> getChunkBuffers() {
        return chunkBuffers.values();
    }
    
    public boolean[] getOctreeGroups() {
        return octreeGroups;
    }
    
    public void clear() {
        chunkBuffers.clear();
        octreeGroups = new boolean[256];
    }
}
