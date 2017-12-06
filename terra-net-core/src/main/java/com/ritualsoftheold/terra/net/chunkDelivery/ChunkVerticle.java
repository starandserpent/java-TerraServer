package com.ritualsoftheold.terra.net.chunkDelivery;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;


/**
 * Chunk verticle class handles the sending/recieving/updating (from the network side) chunks
 */
public class ChunkVerticle extends AbstractVerticle {
    //TODO: We are going to need a way to get notified that a player has joined, moved, any action that will require and update to the chunk data
    //TODO: Need a way to keep track of all the visible chunks that are visible to each player
    //TODO: Send the chunks in form of Position, Priority?, and RLE encoding of the chunk
    //TODO: Im gonna need a way for the players to get their own specific chunks according to their location


    @Override
    public void start() throws Exception {
        EventBus eb = vertx.eventBus();

        //handler for when the player initially joins
        eb.consumer("terra.net.chunkDelivery.playerJoin", message->{

        });

        //handler for when the player moves, get the relevant chunks from the relevant class
        eb.consumer("terra.net.chunkDelivery.playerMove",message->{

        });

        //handler for when any of the chunks that are visible for any of the players needs to be update
        eb.consumer("terra.net.chunkDelivery.playerUpdate",message->{

        });
    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }
}
