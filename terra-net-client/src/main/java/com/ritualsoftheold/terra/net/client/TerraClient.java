package com.ritualsoftheold.terra.net.client;

import org.agrona.concurrent.IdleStrategy;

import com.ritualsoftheold.terra.manager.world.OffheapWorld;

import io.aeron.Aeron;
import io.aeron.FragmentAssembler;
import io.aeron.Subscription;
import io.aeron.logbuffer.FragmentHandler;

public class TerraClient {
    
    private final FragmentHandler handler;
    
    private final IdleStrategy idleStrategy;
    
    private Thread pollThread;
    
    public TerraClient(OffheapWorld world, IdleStrategy idleStrategy) {
        this.handler = new FragmentAssembler(new TerraFragmentHandler(world.getOctreeStorage(),
                world.getChunkStorage(), world.createVerifier()));
        this.idleStrategy = idleStrategy;
    }
    
    public void subscribe(Aeron aeron, String channel, int stream) {
        Subscription sub = aeron.addSubscription(channel, stream);
        pollThread = new PollThread(sub);
        pollThread.start();
    }
    
    private class PollThread extends Thread {
        
        private final Subscription sub;
        
        public PollThread(Subscription sub) {
            this.sub = sub;
        }
        
        @Override
        public void run() {
            int readCount = sub.poll(handler, 10);
            idleStrategy.idle(readCount);
        }
    }
}
