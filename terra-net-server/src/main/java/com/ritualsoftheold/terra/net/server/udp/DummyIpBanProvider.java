package com.ritualsoftheold.terra.net.server.udp;

import io.vertx.core.net.SocketAddress;

/**
 * Doesn't implement IP ban support.
 *
 */
public class DummyIpBanProvider implements IpBanProvider {

    @Override
    public boolean isBanned(SocketAddress address) {
        return false;
    }

}
