package com.ritualsoftheold.terra.net.server.udp;

import com.ritualsoftheold.terra.net.udp.PartialMessage;

import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.SocketAddress;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

/**
 * Handles packets from specific user. Big packets are stitched together.
 *
 */
public class UserPacketHandler {
    
    /**
     * Address of user connection this represents.
     */
    private SocketAddress address;
    
    /**
     * Milliseconds since epoch of the moment when last keep alive packet
     * was received. This might also be time this handler was created.
     */
    private long lastKeepAlive;
    
    private Long2ObjectMap<PartialMessage> partialMessages;
    
    public void handlePacket(Buffer data) {
        
    }
}
