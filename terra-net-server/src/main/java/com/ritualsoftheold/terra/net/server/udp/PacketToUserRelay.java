package com.ritualsoftheold.terra.net.server.udp;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import com.ritualsoftheold.terra.net.udp.NetMagicValues;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramPacket;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.net.SocketAddress;

/**
 * Relays packets to per-user handlers. Handles UDP state changes (connect,
 * disconnect, so on).
 *
 */
public class PacketToUserRelay {
    
    private ConcurrentMap<SocketAddress, UserPacketHandler> handlers;
    
    /**
     * Socket, which is used to send responses to users.
     */
    private DatagramSocket socket;
    
    public void transmit(DatagramPacket packet) {
        SocketAddress sender = packet.sender();
        Buffer data = packet.data();
        
        UserPacketHandler handler = handlers.get(sender);
        if (handler == null) { // That user is not connected
            tryCreateConnection(sender, data);
        } else {
            
        }
    }
    
    public void tryCreateConnection(SocketAddress sender, Buffer data) {
        if (data.getByte(0) == NetMagicValues.ID_CONNECT) {
            // Only put if absent (thread safety)
            handlers.putIfAbsent(sender, createUserPacketHandler(sender));
        }
        // TODO handle other packet types
    }
    
    public UserPacketHandler createUserPacketHandler(SocketAddress address) {
        return null; // TODO
    }
}
