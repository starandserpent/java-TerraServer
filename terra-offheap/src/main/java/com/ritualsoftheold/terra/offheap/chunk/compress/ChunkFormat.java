package com.ritualsoftheold.terra.offheap.chunk.compress;

import com.ritualsoftheold.terra.buffer.BlockBuffer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkType;
import com.ritualsoftheold.terra.offheap.data.WorldDataFormat;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk.Storage;

public interface ChunkFormat extends WorldDataFormat {
    
    public static ChunkFormat forType(byte type) {
        switch (type) {
            case ChunkType.EMPTY:
                return EmptyChunkFormat.INSTANCE;
            case ChunkType.UNCOMPRESSED:
                return UncompressedChunkFormat.INSTANCE;
            case ChunkType.PALETTE16:
                return Palette16ChunkFormat.INSTANCE;
            default:
                throw new IllegalArgumentException("unknown chunk type " + type);
        }
    }
    
    /**
     * Converts chunk data from given storage to another format.
     * @param origin Original storage of chunk data.
     * @param format New format.
     * @param allocator Allocator of the buffer where chunk is located.
     * @return Storage with converted data or null if conversion to given data
     * type is not supported.
     */
    Storage convert(Storage origin, ChunkFormat format, ChunkBuffer.Allocator allocator);
    
    /**
     * Processes change queries in a chunk which has same type as this.
     * <p>
     * You are allowed to mutate contents of given storage, but it must not,
     * be left in invalid state <b>ever</b>. You must only use writes that are
     * atomic, in the sense that readers will either see your change or not,
     * but will never see changes halfway done.
     * <p>
     * If you need non-atomic access, create a new storage, operate on it
     * and return it when you are done. It will be atomically swapped with
     * the old storage and the old storage's memory will be freed.
     * @param chunk Chunk object. Note that you are NOT allowed to access
     * storage of it, but must use the storage given as parameter to
     * this method.
     * @param storage Storage of chunk data.
     * @param changes Iterator for changes.
     * @return New storage or null if mutating previous storage was enough.
     */
    OffheapChunk.Storage processQueries(OffheapChunk chunk, Storage storage, OffheapChunk.ChangeIterator changes);
    
    int getChunkType();
    
    /**
     * Creates a normal block buffer for this format. Writes are done using
     * change queries, reads may be cached or done immediately.
     * <p>
     * Performance critical <b>readers</b> can usually use this block buffer
     * without any trouble, provided that the accesses are not random.
     * Writers, on the other hand, should acquire exclusive access to data.
     * TODO exclusive access acquire in OffheapChunk
     * @param chunk Chunk whose data is accessed.
     * @return New block buffer.
     */
    BlockBuffer createBuffer(OffheapChunk chunk);
    
    @Override
    default boolean isOctree() {
        return false;
    }
}
