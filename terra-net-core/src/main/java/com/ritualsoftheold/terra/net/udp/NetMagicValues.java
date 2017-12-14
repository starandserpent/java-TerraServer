package com.ritualsoftheold.terra.net.udp;

/**
 * Mainly internal packet ids.
 *
 */
public interface NetMagicValues {
    
    /**
     * Terra's internal (low level) message ids.
     */
    public static final byte ID_CONNECT = 0, ID_KEEP_ALIVE = 1, ID_DISCONNECT = 2, ID_DATA = 3;
    
    /**
     * Instructs connection to verify packet contents. Packet must contain
     * a 4-byte long CRC32 hash of their other contents.
     */
    public static final int FLAG_VERIFY = 1 << 7;
    
    /**
     * Instructs receiver to confirm that this packet has been received.
     * If it is a part of larger message, confirmation can of course
     * sent once all of message should have been received.
     */
    public static final int FLAG_RELIABLE = 1 << 6;
    
    /**
     * Indicates that the message has been split in multiple packets.
     * Packet must contain following header:
     * <ol>
     * <li>4 bytes: packet id
     * <li>4 bytes: total packet count of message
     * <li>4 bytes: index of this packet
     * </ol>
     */
    public static final int FLAG_PARTIAL = 1 << 5;
    
    /**
     * TODO
     */
    public static final int FLAG_RELAY = 1 << 4;
    
}
