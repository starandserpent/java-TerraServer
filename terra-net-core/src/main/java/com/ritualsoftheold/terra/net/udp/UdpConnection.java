package com.ritualsoftheold.terra.net.udp;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.SocketAddress;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

/**
 * Manages an UDP based connection.
 *
 */
public class UdpConnection implements NetMagicValues {
    
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
        byte type = data.getByte(0);
        switch (type) {
        
        }
    }
}
