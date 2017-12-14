package com.ritualsoftheold.terra.net.udp;

/**
 * Limitations to preserve safety.
 *
 */
public class NetLimits {
    
    /**
     * Maximum message size the connection will accept. Mainly applicable for
     * partial packet message delivery.
     */
    public int maxMessageSize;
}
