package com.ritualsoftheold.terra.net.client;

import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkStorage;
import com.ritualsoftheold.terra.offheap.verifier.TerraVerifier;
import com.starandserpent.venom.MessageHandler;
import com.starandserpent.venom.UdpConnection;

import io.netty.buffer.ByteBuf;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Handles chunks sent over network.
 *
 */
public class ChunkMsgHandler implements MessageHandler {
    
    private static final Memory mem = OS.memory();
    
    private ChunkStorage storage;
    
    private TerraVerifier verifier;
    
    public ChunkMsgHandler(ChunkStorage storage, TerraVerifier verifier) {
        this.storage = storage;
        this.verifier = verifier;
    }
    
    @Override
    public void receive(UdpConnection conn, ByteBuf msg, byte flags) {
        byte type = msg.readByte();
        int chunkId = msg.readInt();
        verifier.verifyChunkId(chunkId); // Make sure it is safe
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
