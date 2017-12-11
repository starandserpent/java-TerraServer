package com.ritualsoftheold.terra.net.udp;

import io.vertx.core.net.SocketAddress;

/**
 * Checks if an address is banned.
 *
 */
public interface IpBanProvider {
    
    /**
     * Checks if the IP is banned.
     * @param address Address.
     * @return Is it banned.
     */
    boolean isBanned(SocketAddress address);
}
