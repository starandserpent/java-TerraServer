package com.ritualsoftheold.terra.net.client;

import com.ritualsoftheold.terra.offheap.octree.OctreeStorage;
import com.starandserpent.venom.MessageHandler;
import com.starandserpent.venom.UdpConnection;

import io.netty.buffer.ByteBuf;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

public class OctreeMsgHandler implements MessageHandler {
    
    private static final Memory mem = OS.memory();

    private OctreeStorage storage;
    
    @Override
    public void receive(UdpConnection conn, ByteBuf msg, byte flags) {
        byte count = msg.readByte();
        for (int i = 0; i < count; i++) {
            int id = msg.readInt();
            // TODO
        }
    }

}
