package com.ritualsoftheold.terra.net.client;

import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.octree.OctreeStorage;
import com.ritualsoftheold.terra.offheap.verifier.TerraVerifier;
import com.starandserpent.venom.MessageHandler;
import com.starandserpent.venom.UdpConnection;

import io.netty.buffer.ByteBuf;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

public class OctreeMsgHandler implements MessageHandler {
    
    private static final Memory mem = OS.memory();

    private OctreeStorage storage;
    
    private TerraVerifier verifier;
    
    @Override
    public void receive(UdpConnection conn, ByteBuf msg, byte flags) {
        byte count = msg.readByte();
        for (int i = 0; i < count; i++) {
            // Metadata about the octree
            int id = msg.readInt();
            verifier.verifyOctreeId(id); // Id must be safe
            boolean subChunks = msg.readByte() != 0;
            
            // Actual octree data
            long addr = msg.memoryAddress() + msg.readerIndex();
            verifier.verifyOctree(addr, subChunks); // That must be safe too
            msg.readerIndex(msg.readerIndex() + DataConstants.OCTREE_SIZE); // To next octree
            
            // Data is safe, add it to storage
            long octreeAddr = storage.getOctreeAddr(id); // Acquire pointer to group
            mem.copyMemory(addr, octreeAddr, 33); // Copy this octree data there
        }
    }

}
