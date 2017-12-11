package com.ritualsoftheold.terra.net.server.udp;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.datagram.DatagramPacket;
import io.vertx.core.datagram.DatagramSocket;

/**
 * Listens to UDP traffic.
 *
 */
public class UdpListener implements Handler<AsyncResult<DatagramSocket>> {
    
    private DatagramSocket socket;
    private PacketHandler packetHandler;
    
    private PacketToUserRelay relay;
    private IpBanProvider banProvider;
    
    @Override
    public void handle(AsyncResult<DatagramSocket> event) {
        if (event.succeeded()) {
            socket.handler(packetHandler);
        }
    }
    
    private class PacketHandler implements Handler<DatagramPacket> {

        @Override
        public void handle(DatagramPacket packet) {
            if (!banProvider.isBanned(packet.sender())) {
                relay.transmit(packet);
            }
        }
        
    }

}
