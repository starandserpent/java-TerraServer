package com.ritualsoftheold.terra.net;

import com.starandserpent.venom.listeners.MessageType;
import com.starandserpent.venom.listeners.MessageTypes;

/**
 * Contains message types used by Terra's networking components.
 *
 */
public class TerraMessages {
    
    private static final MessageTypes types = new MessageTypes();
    
    /**
     * Sent by server.
     * <ul>
     * <li>1 byte: Terra protocol version
     * </ul>
     */
    public static final MessageType VERSION_INFO = types.create();
    
    /**
     * Sent by server.
     * <ul>
     * <li>1 byte: octree count
     * </ul>
     * For each octree:
     * <ul>
     * <li>4 bytes: octree id
     * <li>33 bytes: raw octree data
     * </ul>
     */
    public static final MessageType OCTREE_DELIVERY = types.create();
    
    /**
     * Delivers a single chunk.
     * <ul>
     * <li>1 byte: chunk type
     * <li>4 bytes: chunk id
     * <li>4 bytes: data length
     * <li>rest: the chunk data
     * </ul>
     */
    public static final MessageType CHUNK_DELIVERY = types.create();
    
    /**
     * TODO implement support for replicating world changes to clients.
     * That should come after not-so-changeable world works reliably.
     */
    public static final MessageType BLOCK_DELTAS = types.create();
    
    public static MessageTypes getTypes() {
        return types;
    }
}
