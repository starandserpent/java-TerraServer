package com.ritualsoftheold.terra.net.udp;

import io.vertx.core.buffer.Buffer;

/**
 * Represents a message that has not been fully transmitted yet.
 *
 */
public class PartialMessage {
    
    /**
     * Parts of message. The ones which have not been received yet are nulls.
     */
    private Buffer[] parts;
    
    // TODO
}
