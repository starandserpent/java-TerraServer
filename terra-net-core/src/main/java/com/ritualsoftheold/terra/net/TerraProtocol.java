package com.ritualsoftheold.terra.net;

/**
 * Defines some important things about Terra's protocol.
 *
 */
public class TerraProtocol {
    
    /**
     * Protocol version must match on client and server.
     */
    public static final int PROTOCOL_VERSION = 0;
    
    public static final byte MESSAGE_TYPE_OCTREE = 0, MESSAGE_TYPE_CHUNK = 1;
    
    public static final int AERON_STREAM = 0;
}
