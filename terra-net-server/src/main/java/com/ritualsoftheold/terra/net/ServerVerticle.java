package com.ritualsoftheold.terra.net;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class ServerVerticle extends AbstractVerticle {
    private final Logger LOG = LoggerFactory.getLogger(ServerVerticle.class);

    private DatagramSocket socket = null;


    @Override
    public void start() throws Exception {
        this.socket = vertx.createDatagramSocket();
        this.socket.listen(8888,"127.0.0.1",event -> {
            if(event.succeeded()){
               socket.handler(handler -> {
                  System.out.println("Recieved message for: "+handler.sender()+" with contents: "+handler.data());

               });
            }else{
                System.out.println("Something seems to have gone wrong");
            }

        });
    }
}
