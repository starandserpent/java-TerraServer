package com.ritualsoftheold.terra.net.server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.ritualsoftheold.terra.net.TerraMessages;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;
import com.ritualsoftheold.terra.offheap.world.WorldLoadListener;
import com.ritualsoftheold.terra.world.LoadMarker;
import com.starandserpent.venom.NetMagicValues;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

/**
 * Sends world data to onservers.
 *
 */
public class SendingLoadListener implements WorldLoadListener, NetMagicValues {
    
    /**
     * World which we are handling sending for.
     */
    private OffheapWorld world;
    
    /**
     * Our observers, mapped by their load markers.
     */
    private ConcurrentMap<LoadMarker,WorldObserver> markersToObservers;
    
    /**
     * Netty buffer allocator.
     */
    private ByteBufAllocator alloc;
    
    public SendingLoadListener(OffheapWorld world, ByteBufAllocator alloc) {
        this.world = world;
        this.markersToObservers = new ConcurrentHashMap<>();
        this.alloc = alloc;
    }
    
    public void addObserver(WorldObserver observer) {
        markersToObservers.put(observer.getLoadMarker(), observer);
    }
    
    public void removeObserver(WorldObserver observer) {
        markersToObservers.remove(observer.getLoadMarker());
    }

    @Override
    public void octreeLoaded(long addr, long groupAddr, int id, float x, float y, float z, float scale,
            LoadMarker trigger) {
        System.out.println("Octree, addr: " + (addr) + ", scale: " + scale);
        WorldObserver observer = markersToObservers.get(trigger);
        if (observer == null) {
            return;
        }
        
        observer.octreeLoaded(alloc, addr, id, scale); // Observer does bulk sending
    }

    @Override
    public void chunkLoaded(OffheapChunk chunk, float x, float y, float z, LoadMarker trigger) {
        System.out.println("Chunk: " + chunk.getIndex());
        WorldObserver observer = markersToObservers.get(trigger);
        if (observer == null) {
            return;
        }
        
        int len = chunk.memoryLength();
        ByteBuf msg = alloc.buffer(len + 9); // 9 for header
        
        try (OffheapChunk.Storage storage = chunk.getStorage()) {
            msg.writeByte(storage.format.getChunkType()); // Chunk type
            msg.writeInt(chunk.getChunkBuffer().getId() << 16 | chunk.getIndex()); // Full id of the chunk
            msg.writeInt(len); // Chunk data length
            world.copyChunkData(chunk.getIndex(), msg.memoryAddress() + 9); // World helper to copy the chunk
            msg.writerIndex(len + 9); // Move writer index to end of data
        }
        
        // Push data to observer, potentially in multiple parts, reliably and don't forget to get a receipt
        TerraMessages.OCTREE_DELIVERY.send(observer.getConnection(), msg, FLAG_PARTIAL | FLAG_RELIABLE | FLAG_VERIFY);
        observer.getConnection().flush(); // FLAG_RELIABLE mandates immediate flushing currently
    }
    
    @Override
    public void finished(LoadMarker trigger) {
        WorldObserver observer = markersToObservers.get(trigger);
        if (observer == null) {
            return;
        }
        observer.octreesFinished(alloc);
    }
}
