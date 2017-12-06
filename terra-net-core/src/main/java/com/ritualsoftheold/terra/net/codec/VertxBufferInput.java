package com.ritualsoftheold.terra.net.codec;

import com.esotericsoftware.kryo.io.Input;

import io.vertx.core.buffer.Buffer;

public class VertxBufferInput extends Input {
    // TODO override every single method from Input
    // They operate on input stream or byte array - we don't want that
    
    /**
     * Vert.x buffer. All operations should be done to this buffer.
     */
    private Buffer buffer;
    
    public VertxBufferInput(Buffer buffer) {
        this.buffer = buffer;
    }
}
