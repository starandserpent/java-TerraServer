package com.ritualsoftheold.terra.offheap.world;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.util.IntFlushList;
import com.ritualsoftheold.terra.world.LoadMarker;

public class OffheapLoadMarker extends LoadMarker implements Cloneable {
        
    private final ConcurrentMap<Integer, ChunkBufferUsers> chunkBuffers;
    
    private final IntFlushList octrees;
    
    public static class ChunkBufferUsers {
        
        private static final VarHandle usersVar;
        
        static {
            try {
                usersVar = MethodHandles.lookup().findVarHandle(ChunkBufferUsers.class, "userCount", int.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new Error(e);
            }
        }
        
        private int userCount;
        
        public int getUserCount() {
            return userCount;
        }
        
        public void addUsers(int i) {
            usersVar.getAndAdd(this, i);
        }
    }
    
    protected OffheapLoadMarker(float x, float y, float z, float hardRadius, float softRadius, int priority) {
        super(x, y, z, hardRadius, softRadius, priority);
        this.chunkBuffers = new ConcurrentHashMap<>();
        this.octrees = new IntFlushList(64, 2); // TODO tune these settings
    }
    
    /**
     * Clone constructor.
     * @param another Another to clone.
     */
    protected OffheapLoadMarker(OffheapLoadMarker another) {
        super(another.getX(), another.getY(), another.getZ(), another.getHardRadius(), another.getSoftRadius(), another.getPriority());
        this.chunkBuffers = new ConcurrentHashMap<>(another.chunkBuffers);
        this.octrees = another.octrees.clone();
    }
    
    public void addBuffer(ChunkBuffer buf) {
        ChunkBufferUsers users = chunkBuffers.computeIfAbsent(buf.getId(), (id) -> new ChunkBufferUsers());
        users.addUsers(1);
    }
    
    public void addOctree(int id) {
        octrees.add(id);
    }
    
    public Map<Integer, ChunkBufferUsers> getChunkBuffers() {
        return chunkBuffers;
    }
    
    public int[] getOctrees() {
        return octrees.getArray();
    }
    
    public void clear() {
        chunkBuffers.clear();
        octrees.clear();
    }
    
    public OffheapLoadMarker clone() {
        return new OffheapLoadMarker(this);
    }
}
