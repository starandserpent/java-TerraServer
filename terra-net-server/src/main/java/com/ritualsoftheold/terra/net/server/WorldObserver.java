package com.ritualsoftheold.terra.net.server;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import com.ritualsoftheold.terra.net.TerraProtocol;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.Pointer;
import com.ritualsoftheold.terra.world.LoadMarker;

import io.aeron.Publication;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Something that can observe a world over network.
 *
 */
public class WorldObserver {
    
    // TODO do NOT allocate with ByteBuffer.allocateDirect
    
    private static final Memory mem = OS.memory();
    
    private LoadMarker marker;
    
    private Publication publication;
    
    private @Pointer AtomicLong pendingOctrees;
    
    private byte octreesPerPacket;
    
    private byte pendingIndex;
    
    public WorldObserver(LoadMarker marker, Publication publication, byte octreesPerPacket) {
        this.marker = marker;
        this.publication = publication;
        this.pendingOctrees = new AtomicLong(mem.allocate(octreesPerPacket * (DataConstants.OCTREE_SIZE + 4 + 1)));
        this.octreesPerPacket = octreesPerPacket;
        this.pendingIndex = 0;
    }
    
    public LoadMarker getLoadMarker() {
        return marker;
    }
    
    public Publication getPublication() {
        return publication;
    }
    
    private long lockOctrees() {
        while (true) {
            long addr = pendingOctrees.getAndSet(0);
            if (addr != 0) { // We got it!
                return addr;
            }
            Thread.onSpinWait();
        }
    }
    
    private void unlockOctrees(long addr) {
        pendingOctrees.set(addr);
    }
    
    void octreeLoaded(long octree, int id, float scale) {
        // Get exclusive access to address
        long addr = lockOctrees();
        
        // Make sure we have buffer where there is space
        int entrySize = DataConstants.OCTREE_SIZE + 4 + 1;
        int len = octreesPerPacket * entrySize;
        if (pendingIndex == octreesPerPacket) { // Need next packet!
            // Send the buffer we have to client
            MutableDirectBuffer buf = new UnsafeBuffer(ByteBuffer.allocateDirect(len + 2));
            buf.putByte(0, TerraProtocol.MESSAGE_TYPE_OCTREE);
            buf.putByte(1, octreesPerPacket);
            mem.copyMemory(addr, buf.addressOffset() + 2, len);
            publication.offer(buf);
            mem.freeMemory(addr, len); // Free old buffer
            
            // Allocate new buffer
            addr = mem.allocate(len);
            pendingIndex = 0; // To start
        }
        
        // Write octree id and data to address we have
        mem.writeInt(addr + pendingIndex * entrySize, id); // Octree id
        mem.writeByte(addr + pendingIndex * entrySize + 4, (byte) (scale < DataConstants.CHUNK_SCALE * 2 ? 1 : 0)); // If subnodes are chunks
        mem.copyMemory(octree, addr + pendingIndex * entrySize + 5, DataConstants.OCTREE_SIZE); // Octree data
        pendingIndex++; // Next free slot
        
        // Put address back, thus ending spin wait
        unlockOctrees(addr);
    }

    void octreesFinished() {
        long addr = lockOctrees();
        
        // Send the buffer we have to client (filled parts)
        int len = pendingIndex * (DataConstants.OCTREE_SIZE + 4 + 1);
        MutableDirectBuffer buf = new UnsafeBuffer(ByteBuffer.allocateDirect(len + 1));
        buf.putByte(0, TerraProtocol.MESSAGE_TYPE_OCTREE);
        buf.putByte(1, pendingIndex);
        mem.copyMemory(addr, buf.addressOffset() + 2, len);
        publication.offer(buf);
        mem.freeMemory(addr, len); // Free old buffer
        
        // Allocate new buffer
        addr = mem.allocate(len);
        pendingIndex = 0; // To start
        
        unlockOctrees(addr);
    }
}
