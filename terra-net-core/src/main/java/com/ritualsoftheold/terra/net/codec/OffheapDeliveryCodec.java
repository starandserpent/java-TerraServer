package com.ritualsoftheold.terra.net.codec;

import com.ritualsoftheold.terra.net.message.ChunkDeliveryMessage;
import com.ritualsoftheold.terra.net.message.OffheapDeliveryMessage;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

/**
 * Transmits offheap data over network. Uses OffheapDeliveryMessage as a
 * container that exposes address and data length.
 *
 */
public class OffheapDeliveryCodec implements MessageCodec<OffheapDeliveryMessage, OffheapDeliveryMessage> {
    
    // TODO WIP
    
    @Override
    public void encodeToWire(Buffer buffer, OffheapDeliveryMessage s) {
        
    }

    @Override
    public OffheapDeliveryMessage decodeFromWire(int pos, Buffer buffer) {
        byte type = buffer.getByte(pos);
        
        OffheapDeliveryMessage msg = null;
        pos++;
        switch (type) {
            case 0: // Chunk delivery
                msg = decodeChunkDelivery(pos, buffer);
                break;
            case 1: // Octree delivery
                break;
        }
        
        return msg;
    }
    
    private ChunkDeliveryMessage decodeChunkDelivery(int pos, Buffer buffer) {
        int chunkCount = buffer.getInt(pos); // Chunk count
        int length = buffer.getInt(pos + 4); // Total data length
        pos += 4;
        
        // Offsets
        int[] offsets = new int[chunkCount];
        for (int i = 0; i < chunkCount; i++) {
            offsets[i] = buffer.getInt(pos);
            pos += 4;
        }
        
        // Chunk ids
        int[] ids = new int[chunkCount];
        for (int i = 0; i < chunkCount; i++) {
            ids[i] = buffer.getInt(pos);
            pos += 4;
        }
        
        // TODO validate data; allocate memory
        return new ChunkDeliveryMessage(0, length, chunkCount, offsets, ids);
    }

    @Override
    public OffheapDeliveryMessage transform(OffheapDeliveryMessage s) {
        return s;
    }

    @Override
    public String name() {
        return "offheap-delivery";
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }
    
}
