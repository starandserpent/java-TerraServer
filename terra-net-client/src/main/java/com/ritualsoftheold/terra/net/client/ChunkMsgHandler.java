package com.ritualsoftheold.terra.net.client;

import java.lang.invoke.VarHandle;

import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkStorage;
import com.ritualsoftheold.terra.offheap.chunk.compress.ChunkFormat;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;
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
        verifier.verifyChunkId(chunkId); // Make sure id is safe
        int length = msg.readInt();
        verifier.verifyChunkLength(length, msg.readableBytes());
        
        ChunkBuffer buf = storage.getOrLoadBuffer(chunkId >>> 16); // Get the buffer
        
        // Fill in the chunk in the buffer
        OffheapChunk chunk = buf.getChunk(chunkId & 0xffff);
        long addr = mem.allocate(length);
        mem.copyMemory(msg.memoryAddress() + msg.readerIndex(), addr, length);
        VarHandle.fullFence(); // Ensure that loads are not reordered in middle of copyMemory
        // (after chunk is available, that is)
        
        OffheapChunk.Storage storage = new OffheapChunk.Storage(ChunkFormat.forType(type), addr, length);
        chunk.setStorageInternal(storage); // This makes chunk available
    }

}
