package com.ritualsoftheold.terra.net.codec;

import com.ritualsoftheold.terra.net.message.OffheapDeliveryMessage;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

/**
 * Transmits offheap data over network. Uses OffheapDeliveryMessage as a
 * container that exposes address and data length.
 *
 */
public class OffheapDeliveryCodec implements MessageCodec<OffheapDeliveryMessage, OffheapDeliveryMessage> {
    
    // TODO write this class to send and receive data over network
    
    @Override
    public void encodeToWire(Buffer buffer, OffheapDeliveryMessage s) {
        
    }

    @Override
    public OffheapDeliveryMessage decodeFromWire(int pos, Buffer buffer) {
        return null;
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
