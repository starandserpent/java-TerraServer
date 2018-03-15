package com.ritualsoftheold.terra.net.server;

import java.util.Map;

import com.ritualsoftheold.terra.offheap.node.OffheapChunk;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;
import com.ritualsoftheold.terra.offheap.world.WorldLoadListener;
import com.ritualsoftheold.terra.world.LoadMarker;
import com.starandserpent.venom.NetMagicValues;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Sends world data to onservers.
 *
 */
public class SendingLoadListener implements WorldLoadListener, NetMagicValues {
    
    private static final Memory mem = OS.memory();
    
    private OffheapWorld world;
    
    private Map<LoadMarker,WorldObserver> markersToObservers;
    
    private ByteBufAllocator alloc;

    @Override
    public void octreeLoaded(long addr, long groupAddr, int id, float x, float y, float z, float scale,
            LoadMarker trigger) {
        WorldObserver observer = markersToObservers.get(trigger);
        if (observer == null) {
            return;
        }
        
        // TODO merge many octrees to one packet
    }

    @Override
    public void chunkLoaded(OffheapChunk chunk, float x, float y, float z, LoadMarker trigger) {
        WorldObserver observer = markersToObservers.get(trigger);
        if (observer == null) {
            return;
        }
        
        int len = chunk.memoryLength();
        ByteBuf msg = alloc.buffer(len + 5); // 5 for header
        
        msg.writeByte(chunk.getBuffer().getChunkType(chunk.getBufferId())); // Chunk type
        msg.writeInt(len); // Chunk data length
        world.copyChunkData(chunk.getBufferId(), msg.memoryAddress() + 5); // World helper to copy the chunk
        msg.writerIndex(len + 5); // Move writer index to end of data
        
        // Push data to observer, potentially in multiple parts, reliably and don't forget to get a receipt
        observer.getConnection().sendMessage(msg, (byte) (FLAG_PARTIAL | FLAG_RELIABLE | FLAG_VERIFY));
    }
    
}
