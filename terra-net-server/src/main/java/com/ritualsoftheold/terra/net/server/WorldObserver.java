package com.ritualsoftheold.terra.net.server;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.world.LoadMarker;
import com.starandserpent.venom.NetMagicValues;
import com.starandserpent.venom.UdpConnection;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Something that can observe a world over network.
 *
 */
public class WorldObserver implements NetMagicValues {
    
    private static final Memory mem = OS.memory();
    
    private LoadMarker marker;
    
    private UdpConnection conn;
    
    private AtomicLong pendingOctrees;
    
    private int octreesPerPacket;
    
    private int pendingIndex;
    
    public WorldObserver(LoadMarker marker, UdpConnection conn, int octreesPerPacket) {
        this.marker = marker;
        this.conn = conn;
        this.pendingOctrees = new AtomicLong(mem.allocate(octreesPerPacket * (DataConstants.OCTREE_SIZE + 4)));
        this.octreesPerPacket = octreesPerPacket;
        this.pendingIndex = 0;
    }
    
    public LoadMarker getLoadMarker() {
        return marker;
    }
    
    public UdpConnection getConnection() {
        return conn;
    }
    
    private long lockOctrees() {
        while (true) {
            long addr = pendingOctrees.getAndSet(0);
            if (addr != 0) { // We got it!
                return addr;
            }
        }
    }
    
    private void unlockOctrees(long addr) {
        pendingOctrees.set(addr);
    }
    
    void octreeLoaded(ByteBufAllocator alloc, long octree, int id) {
        // Get exclusive access to address
        long addr = lockOctrees();
        
        // Make sure we have buffer where there is space
        int entrySize = DataConstants.OCTREE_SIZE + 4;
        int len = octreesPerPacket * entrySize;
        boolean doFlush = false;
        if (pendingIndex == octreesPerPacket) { // Need next packet!
            // Send the buffer we have to client
            ByteBuf buf = alloc.buffer(len + 1);
            buf.writeByte(octreesPerPacket);
            mem.copyMemory(addr, buf.memoryAddress() + 1, len);
            buf.writerIndex(len + 1);
            conn.sendMessage(buf, FLAG_RELIABLE | FLAG_VERIFY); // Send copied old buffer
            mem.freeMemory(addr, len); // Free old buffer
            
            // Allocate new buffer
            addr = mem.allocate(len);
            pendingIndex = 0; // To start
            doFlush = true; // Call flush on connection
        }
        
        // Write octree id and data to address we have
        mem.writeInt(addr + pendingIndex * entrySize, id);
        mem.copyMemory(octree, addr + pendingIndex * entrySize + 4, DataConstants.OCTREE_SIZE);
        pendingIndex++; // Next free slot
        
        // Put address back, thus ending spin wait
        unlockOctrees(addr);
        
        conn.flush(); // Flush after unlocking, might allow better concurrency
    }

    void octreesFinished(ByteBufAllocator alloc) {
        long addr = lockOctrees();
        
        // Send the buffer we have to client (filled parts)
        int len = pendingIndex * (DataConstants.OCTREE_SIZE + 4);
        ByteBuf buf = alloc.buffer(len + 1);
        buf.writeByte(pendingIndex);
        mem.copyMemory(addr, buf.memoryAddress() + 1, len);
        buf.writerIndex(len + 1);
        conn.sendMessage(buf, FLAG_RELIABLE | FLAG_VERIFY); // Send copied old buffer
        mem.freeMemory(addr, len); // Free old buffer
        
        // Allocate new buffer
        addr = mem.allocate(len);
        pendingIndex = 0; // To start
        
        unlockOctrees(addr);
        
        // Flush the connection after we've released lock
        conn.flush();
    }
}
