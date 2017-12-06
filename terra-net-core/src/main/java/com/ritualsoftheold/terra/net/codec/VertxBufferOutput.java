package com.ritualsoftheold.terra.net.codec;

import com.esotericsoftware.kryo.io.Output;

import io.vertx.core.buffer.Buffer;

/**
 * Output for Kryo that implements Vert.x buffer as backend.
 *
 */
public class VertxBufferOutput extends Output {
    // TODO override every single method from Output
    // They operate on output stream or byte array - we don't want that
    
    /**
     * Vert.x buffer. All operations should be done to this buffer.
     */
    private Buffer buffer;
    
    public VertxBufferOutput(Buffer buffer) {
        this.buffer = buffer;
    }
}
