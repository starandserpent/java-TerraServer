package com.ritualsoftheold.terra.offheap.chunk.compress;

public interface ChunkFormat {
    
    public static ChunkFormat forType(byte type) {
        switch (type) {
            default:
                throw new IllegalArgumentException("unknown chunk type " + type);
        }
    }
    
    /**
     * Attempts to convert given chunk from type that this format
     * supports to another one.
     * @param from Source memory address.
     * @param to Target memory address.
     * @param type Chunk type.
     * @return If given type is unsupported, false is returned.
     */
    boolean convert(long from, long to, int type);
    
    /**
     * Processes given queries for given chunk. Note that chunk must have
     * type which this format is meant for.
     * @param chunk Address to chunk data.
     * @param queue Address to query queue.
     * @param size Size of query data.
     */
    void processQueries(long chunk, long queue, int size);
}
