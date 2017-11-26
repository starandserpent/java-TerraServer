package com.ritualsoftheold.terra.offheap.chunk.compress;

import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.ChunkType;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer.Allocator;

import it.unimi.dsi.fastutil.shorts.Short2ByteMap;
import it.unimi.dsi.fastutil.shorts.Short2ByteOpenHashMap;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Handles palette based chunk format, which has maximum material count of 256.
 *
 */
public class Palette1ChunkFormat implements ChunkFormat {
    
    // WIP. TODO. This is an experiment
    
    private static final Memory mem = OS.memory();
    
    private static final int DATA_LENGTH = 1 + 256 * 2 + DataConstants.CHUNK_MAX_BLOCKS;
    
    @Override
    public boolean convert(long from, long to, int type) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void processQueries(long chunk, int chunkLen, Allocator alloc, long queue, int size) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void getBlocks(long chunk, int[] indices, short[] ids, int beginIndex, int endIndex) {
        long palette = chunk + 1;
        long data = palette + 256 * 2;
        for (int i = beginIndex; i < endIndex; i++) {
            byte paletteId = mem.readByte(data + indices[i]); // Get palette id
            short worldId = mem.readShort(palette + paletteId * 2); // Get world id for it
            ids[i] = worldId;
        }
    }

    @Override
    public SetAllResult setAllBlocks(short[] data, Allocator allocator) {
        long addr = allocator.alloc(DATA_LENGTH);
        long paletteAddr = addr + 1;
        long dataAddr = paletteAddr + 256 * 2;
        Short2ByteMap palette = new Short2ByteOpenHashMap(256);
        byte paletteSize = 0;
        
        // Write palette ids
        for (int i = 0; i < data.length; i++) {
            short id = data[i];
            byte paletteId;
            if (palette.containsKey(id)) { // Already there
                paletteId = palette.get(id);
            } else { // Need to assign to palette
                if (paletteSize == 255) { // We'll have to change the chunk format
                    // Default to uncompressed, anything else is unlikely to work
                    UncompressedChunkFormat.INSTANCE.setAllBlocks(data, allocator.createDummy(addr, DataConstants.CHUNK_UNCOMPRESSED));
                    return new ChunkFormat.SetAllResult(addr, DataConstants.CHUNK_UNCOMPRESSED, ChunkType.UNCOMPRESSED);
                }
                
                paletteId = paletteSize;
                paletteSize++;
                palette.put(id, paletteId);
                
                // Write to offheap too
                // This is inverted from map we have to allow fast block reads in expense of writes
                mem.writeShort(paletteAddr + paletteId * 2, id);
            }
            
            // Write paletteId offheap
            mem.writeByte(dataAddr + i, paletteId);
        }
        
        // Last save palette size
        mem.writeByte(addr, paletteSize);
        
        return new ChunkFormat.SetAllResult(addr, DATA_LENGTH);
    }

    @Override
    public int getChunkType() {
        return ChunkType.PALETTE_1;
    }

}
