package com.ritualsoftheold.terra.net.codec;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.KryoPool;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

/**
 * Uses Kryo to serialize POJOs.
 *
 */
public class KryoCodec implements MessageCodec<Object, Object> {
    
    /**
     * Pool of Kryo instances (which are single-threaded).
     */
    private KryoPool kryoPool;
    
    public KryoCodec(KryoPool kryoPool) {
        this.kryoPool = kryoPool;
    }

    @Override
    public void encodeToWire(Buffer buffer, Object s) {
        Kryo kryo = kryoPool.borrow();
        
        // TODO implement VertxBufferOutput
        
        kryoPool.release(kryo);
    }

    @Override
    public Object decodeFromWire(int pos, Buffer buffer) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object transform(Object s) {
        return s;
    }

    @Override
    public String name() {
        return "terra-kryo";
    }

    @Override
    public byte systemCodecID() {
        return -1; // Terra is library, but only an user of Vert.x
    }

}
