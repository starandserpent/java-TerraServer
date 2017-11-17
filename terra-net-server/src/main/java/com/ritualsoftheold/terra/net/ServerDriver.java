package com.ritualsoftheold.terra.net;

import io.vertx.core.Vertx;

public class ServerDriver {
    public static void main(String ... args){
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new ServerVerticle());
    }
}
