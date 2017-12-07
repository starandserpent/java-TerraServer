package com.ritualsoftheold.terra.net.message;

/**
 * Contains memory address and length for offheap data.
 *
 */
public class OffheapDeliveryMessage {
    
    /**
     * Memory address of data.
     */
    public long addr;
    
    /**
     * Length of data.
     */
    public long length;
    
    public OffheapDeliveryMessage(long addr, long length) {
        this.addr = addr;
        this.length = length;
    }
}
