package com.ritualsoftheold.terra.net.message;

public class ChunkDeliveryMessage extends OffheapDeliveryMessage {
    
    /**
     * Count of chunks in this message.
     */
    public int chunkCount;
    
    /**
     * Chunk offsets in the offheap data. Chunks may belong to different
     * buffers.
     */
    public int[] chunkOffsets;
    
    /**
     * Ids for chunks in this message.
     */
    public int[] chunkIds;
    
    public ChunkDeliveryMessage(long addr, long length, int count, int[] offsets, int[] ids) {
        super(addr, length);
        this.chunkCount = count;
        this.chunkOffsets = offsets;
        this.chunkIds = ids;
    }

}
