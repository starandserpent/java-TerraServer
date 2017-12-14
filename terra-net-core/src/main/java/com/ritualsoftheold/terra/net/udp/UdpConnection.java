package com.ritualsoftheold.terra.net.udp;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.SocketAddress;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

/**
 * Manages an UDP based connection.
 *
 */
public class UdpConnection implements NetMagicValues {
    
    /**
     * Address of the other end of this connection.
     */
    private SocketAddress address;
    
    /**
     * Milliseconds since epoch of the moment when last keep alive packet
     * was received. This might also be time this handler was created.
     */
    private long lastKeepAlive;
    
    private Int2ObjectMap<PartialMessage> partialMessages;
    
    /**
     * Various limits.
     */
    private NetLimits limits;
    
    public void handlePacket(Buffer data) {
        byte type = data.getByte(0);
        switch (type) {
            case ID_CONNECT:
                break; // Do nothing, connection is already there
            case ID_KEEP_ALIVE:
                lastKeepAlive = System.currentTimeMillis();
                break;
            case ID_DISCONNECT:
                // TODO disconnection handling
                break;
            case ID_DATA:
                handleDataPacket(data);
                break;
        }
    }
    
    private void handleDataPacket(Buffer data) {
        int offset = 1;
        int flags = data.getByte(offset);
        
        boolean verify = (flags & FLAG_VERIFY) == 1;
        if (verify) {
            // TODO verify the data
            offset += 32;
        }
        
        boolean reliable = (flags & FLAG_RELIABLE) == 1;
        boolean partial = (flags & FLAG_PARTIAL) == 1;
        
        if (partial) {
            handlePartialPacket(data, offset, reliable);
        } else {
            
        }
    }
    
    private void handlePartialPacket(Buffer data, int offset, boolean reliable) {
        int id = data.getInt(offset);
        PartialMessage msg = partialMessages.get(id);
        if (msg == null) {
            int packetCount = data.getInt(offset + 4) - 1;
            
            msg = new PartialMessage(packetCount, limits.maxMessageSize);
            partialMessages.put(id, msg);
        }
        int index = data.getInt(offset + 8);
        int result = msg.receivePart(id, data.slice(index, data.length() - 1));
        
        if (result > PartialMessage.RESULT_ERROR) {
            partialMessages.remove(id); // Remove the data. Client is trying to DOS us, maybe
        } else {
            if (result == PartialMessage.OK_ALL_DONE) {
                // TODO handle composing the message
            }
            // Otherwise we handled this packet already
        }
    }
}
