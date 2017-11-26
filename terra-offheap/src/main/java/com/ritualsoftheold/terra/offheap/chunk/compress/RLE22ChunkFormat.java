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
    public void processQueries(long chunk, int chunkLen, ChunkBuffer.Allocator alloc, long queue, int size) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void getBlocks(long chunk, int[] indices, short[] ids,
            int beginIndex, int endIndex) {
        int blockIndex = 0;
        int curLookupIndex = beginIndex;
        
        for (int i = 0; true; i++) {
            // Get material and how many of that there are
            short mat = mem.readShort(chunk + i * 4);
            int len = Short.toUnsignedInt(mem.readShort(chunk + i * 4 + 2));
            blockIndex += len;
            
            // Check which of indices we're looking for match
            for (int j = curLookupIndex; true; j++) {
                // Stop there, might be other data in this array!
                if (curLookupIndex == endIndex) {
                    return; // Time to return
                }
                
                int target = indices[j];
                if (target <= blockIndex) { // Hey, this matches
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
