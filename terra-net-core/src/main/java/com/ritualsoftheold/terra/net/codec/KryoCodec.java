package com.ritualsoftheold.terra.net.codec;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.UnsafeMemoryInput;
import com.esotericsoftware.kryo.pool.KryoPool;

import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

/**
 * Uses Kryo to serialize POJOs.
 *
 */
// TODO remove, probably cannot use with UDP
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
        
        kryo.writeClassAndObject(new VertxBufferOutput(buffer), s);
        
        kryoPool.release(kryo);
    }

    @Override
    public Object decodeFromWire(int pos, Buffer buffer) {
        Kryo kryo = kryoPool.borrow();
        
        ByteBuf nettyBuf = buffer.getByteBuf();
        // TODO verify memory safety (no segfaults) with testing!
        UnsafeMemoryInput input = new UnsafeMemoryInput(nettyBuf.memoryAddress() + pos, nettyBuf.capacity() - pos);
        Object object = kryo.readClassAndObject(input);
        
        kryoPool.release(kryo);
        
        return object;
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
