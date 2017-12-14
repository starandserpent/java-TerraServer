package com.ritualsoftheold.terra.net.udp;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import io.vertx.core.buffer.Buffer;

/**
 * Represents a message that has not been fully transmitted yet.
 *
 */
public class PartialMessage {
    
    private static final AtomicIntegerFieldUpdater<PartialMessage> receivedUpdater = AtomicIntegerFieldUpdater.newUpdater(PartialMessage.class, "received");
    private static final AtomicIntegerFieldUpdater<PartialMessage> lengthUpdater = AtomicIntegerFieldUpdater.newUpdater(PartialMessage.class, "receivedLength");
    
    public static final int RESULT_ERROR = 1, ERROR_LENGTH = 1, ERROR_ID = 2, RESULT_OK = 0, OK_ALL_DONE = -1;
    
    /**
     * Parts of message. The ones which have not been received yet are nulls.
     */
    private Buffer[] parts;
    
    /**
     * Packet count of this message.
     */
    private int packetCount;
    
    /**
     * Amount of packets that have been received.
     */
    private int received;
    
    /**
     * Length of data that has been received.
     */
    private int receivedLength;
    
    /**
     * Maximum allowed packet length.
     */
    private int maxLength;
    
    public PartialMessage(int packetCount, int maxLength) {
        this.packetCount = packetCount;
        this.parts = new Buffer[packetCount];
        this.received = 0;
        this.receivedLength = 0;
        this.maxLength = maxLength;
    }
    
    public int receivePart(int id, Buffer buf) {
        if (lengthUpdater.get(this) + buf.length() > maxLength) {
            // Length limit exceeded
            return ERROR_LENGTH;
        }
        if (id > packetCount + 1) {
            // Id exceeds max id
            return ERROR_ID;
        }
        
        int received = receivedUpdater.incrementAndGet(this);
        lengthUpdater.addAndGet(this, buf.length());
        parts[id] = buf;
        
        return received >= packetCount ? RESULT_OK : OK_ALL_DONE;
    }
}
