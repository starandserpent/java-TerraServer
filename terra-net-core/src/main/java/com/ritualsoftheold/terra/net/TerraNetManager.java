package com.ritualsoftheold.terra.net;

import com.esotericsoftware.kryo.pool.KryoPool;
import com.ritualsoftheold.terra.net.codec.KryoCodec;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;

/**
 * Terra's networking routines (using Vert.x)
 *
 */
public class TerraNetManager {
    
    private Vertx vertx;
    
    private KryoPool kryoPool;
    
    public TerraNetManager(Vertx vertx, KryoPool kryoPool) {
        this.vertx = vertx;
        this.kryoPool = kryoPool;
        registerTerraCodecs();
    }
    
    /**
     * Registers some codecs that Terra requires. Other codecs can, of course,
     * be registered after and before this has been called.
     */
    private void registerTerraCodecs() {
        EventBus bus = vertx.eventBus();
        bus.registerDefaultCodec(Object.class, new KryoCodec(kryoPool));
    }
    
    public Vertx getVertx() {
        return vertx;
    }
    
    public KryoPool getKryoPool() {
        return kryoPool;
    }
}
