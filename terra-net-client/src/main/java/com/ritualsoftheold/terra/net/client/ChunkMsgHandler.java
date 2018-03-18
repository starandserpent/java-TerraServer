package com.ritualsoftheold.terra.net.client;

import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkStorage;
import com.starandserpent.venom.MessageHandler;
import com.starandserpent.venom.UdpConnection;

import io.netty.buffer.ByteBuf;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

public class ChunkMsgHandler implements MessageHandler {
    
    private static final Memory mem = OS.memory();
    
    private ChunkStorage storage;
    
    @Override
    public void receive(UdpConnection conn, ByteBuf msg, byte flags) {
        byte type = msg.readByte();
        int chunkId = msg.readInt();
        int len = msg.readInt();
        
        ChunkBuffer buf = storage.getOrLoadBuffer(chunkId >>> 16); // Get the buffer
        
        // Fill in the chunk in the buffer
        int index = chunkId & 0xffff;
        buf.setChunkType(index, type);
        long addr = mem.allocate(len);
        buf.setChunkAddr(index, addr);
        buf.setChunkLength(index, len);
    }

}
