package com.ritualsoftheold.terra.offheap.chunk.compress;

import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkType;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

public class RLE22ChunkFormat implements ChunkFormat {
    
    private static final Memory mem = OS.memory();

    public static final RLE22ChunkFormat INSTANCE = new RLE22ChunkFormat();
    
    private static final int REALLOC_SIZE = DataConstants.CHUNK_UNCOMPRESSED / 32; // TODO measure this
    
    @Override
    public boolean convert(long from, long to, int type) {
        switch (type) {
            case ChunkType.UNCOMPRESSED:
                RunLengthCompressor.decompress(from, to);
                break;
        }
        
        return false;
    }

    @Override
    public ChunkFormat.ProcessResult processQueries(long chunk, int chunkLen, ChunkBuffer.Allocator alloc, long queue, int size) {
        // TODO figure out a way to avoid uncompress-compress (it might be quite costly)
        // This is still better than nothing; uncompress - for all of queue: apply - compress
        // is far better than; for all of queue: uncompress - apply one change - compress
        
        // Delegate stuff to uncompressed chunk format
        long uncompressed = alloc.alloc(DataConstants.CHUNK_UNCOMPRESSED);
        convert(chunk, uncompressed, ChunkType.UNCOMPRESSED);
        UncompressedChunkFormat.INSTANCE.processQueries(uncompressed, DataConstants.CHUNK_UNCOMPRESSED, alloc, queue, size);
        
        // Attempt recompression
        int compressedLen = RunLengthCompressor.compress(uncompressed, chunk, chunkLen);
        if (compressedLen == -1) { // Oops. Compression failed (out of memory)
            long newAddr = alloc.alloc(DataConstants.CHUNK_UNCOMPRESSED);
            compressedLen = RunLengthCompressor.compress(uncompressed, newAddr, DataConstants.CHUNK_UNCOMPRESSED);
            
            // Still fails, need to fall back to uncompressed chunk format
            if (compressedLen == -1) {
                alloc.free(newAddr, DataConstants.CHUNK_UNCOMPRESSED);
                return new ChunkFormat.ProcessResult(DataConstants.CHUNK_UNCOMPRESSED, ChunkType.UNCOMPRESSED, uncompressed);
            }
            
            // Potentially reallocate, third time. If it is "worth it"
            if (DataConstants.CHUNK_UNCOMPRESSED - compressedLen > REALLOC_SIZE) {
                long addr3 = alloc.alloc(compressedLen);
                mem.copyMemory(newAddr, addr3, compressedLen);
                alloc.free(newAddr, DataConstants.CHUNK_UNCOMPRESSED);
                alloc.free(uncompressed, DataConstants.CHUNK_UNCOMPRESSED);
                return new ChunkFormat.ProcessResult(compressedLen, ChunkType.RLE_2_2, addr3);
            } else { // It isn't. Just free uncompressed data and return
                alloc.free(uncompressed, DataConstants.CHUNK_UNCOMPRESSED);
                return new ChunkFormat.ProcessResult(DataConstants.CHUNK_UNCOMPRESSED, ChunkType.RLE_2_2, newAddr);
            }
        }
        
        // Everything went well. How wonderful (and lucky)
        alloc.free(uncompressed, DataConstants.CHUNK_UNCOMPRESSED);
        return new ChunkFormat.ProcessResult(chunkLen, ChunkType.RLE_2_2, chunk);
    }

    @Override
    public void getBlocks(long chunk, int[] indices, short[] ids,
            int beginIndex, int endIndex) {
        int blockIndex = 0;
        int curLookupIndex = beginIndex;
        
        for (int i = 0; true; i++) {
            // Get material and how many of that there are
            int entry = mem.readInt(chunk + i * 4);
            short mat = (short) (entry & 0xffff);
            int len = Short.toUnsignedInt((short) (entry >>> 16)) + 1;
            int minIndex = blockIndex;
            blockIndex += len;
            
            // Check which of indices we're looking for match
            for (int j = curLookupIndex; true; j++) {
                // Stop there, might be other data in this array!
                if (j == endIndex) {
                    return; // Time to return
                }
                
                int target = indices[j];
                if (target >= minIndex && target < blockIndex) { // Hey, this matches
                    // Set material to ids
                    ids[j] = mat;
                } else { // Not there... Next RLE batch, please
                    break;
                }
                curLookupIndex = j; // We'll continue there with next batch of RLE'd data
            }
        }
    }

    @Override
    public ChunkFormat.SetAllResult setAllBlocks(short[] data, ChunkBuffer.Allocator allocator) {
        long addr = allocator.alloc(DataConstants.CHUNK_UNCOMPRESSED);
        
        int offset = 0;
        int count = 0;
        short previous = data[0];
        for (short id : data) {
            // If id changed or max count for this RLE format hit, write entry
            if (previous != id || count == RunLengthCompressor.MAX_COUNT) {
                mem.writeInt(addr + offset, (count - 1) << 16 | previous);
                offset += 4;
                count = 1;
                previous = id;
            } else { // If we can, just add to count to be written somewhere, sometime
                count++;
            }
            
            // Oh. This is not working, we'd use more space than uncompressed chunk
            if (offset == DataConstants.CHUNK_UNCOMPRESSED) {
                // Use uncompressed chunk format to handle this
                UncompressedChunkFormat.INSTANCE.setAllBlocks(data, allocator.createDummy(addr, DataConstants.CHUNK_UNCOMPRESSED));
                return new ChunkFormat.SetAllResult(addr, DataConstants.CHUNK_UNCOMPRESSED, ChunkType.UNCOMPRESSED);
            }
        }
        // Write last entry that might have not been written in the loop
        if (count != 0) {
            mem.writeInt(addr + offset, (count - 1) << 16 | previous);
            offset += 4; // Increase outIndex to point to next data slot
        }
        
        // Maybe reallocate memory, if it is probably worth the cost
        int length = DataConstants.CHUNK_UNCOMPRESSED;
        if (length - offset > REALLOC_SIZE) {
            length = offset; // We reallocate, so length is the old offset
            long newAddr = allocator.alloc(length); // Allocate memory, but only how much we need this time
            mem.copyMemory(addr, newAddr, length);
            allocator.free(addr, DataConstants.CHUNK_UNCOMPRESSED);
            addr = newAddr; // Assign new address in place of old
        }
        
        // Create a sensible result
        return new ChunkFormat.SetAllResult(addr, length);
    }

    @Override
    public int getChunkType() {
        return ChunkType.RLE_2_2;
    }

}
