package com.ritualsoftheold.terra.offheap.chunk.compress;

import com.ritualsoftheold.terra.buffer.BlockBuffer;
import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.material.TerraMaterial;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.Pointer;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * 4 bits per block. Max 16 block types in a chunk.
 *
 */
public class Palette16ChunkFormat implements ChunkFormat {
    
    private static final int PALETTE_SIZE = 16;
    
    private static final int PALETTE_ENTRY_LENGTH = 4;
    
    private static final int PALETTE_LENGTH = PALETTE_SIZE * PALETTE_ENTRY_LENGTH;
    
    private static final Memory mem = OS.memory();
    
    /**
     * Gets index of the byte where given index
     * @param index
     * @return
     */
    private int byteIndex(int index) {
        return index >> 1; // Quick integer divide positive by 2
    }
    
    /**
     * Gets the bit offset in the byte towards which the value should be
     * shifted from right to left.
     * @param index
     * @return
     */
    private int shiftOffset(int index) {
        // Quick modulo positive by 2
        // Get shift offset by reducing (0 -> -1, 1 -> 0)
        // and then multiplying by -4 to get shift offset in bits, positive
        return ((index & 1) - 1) * -4;
    }
    
    /**
     * Finds or create a palette id.
     * @param palette Palette address.
     * @param id World block id.
     * @return Palette id or -1 if palette is exhausted.
     */
    private int findPaletteId(@Pointer long palette, int id) {
        for (int i = 0; i < PALETTE_LENGTH; i++) {
            int worldId = mem.readVolatileInt(palette + i * PALETTE_ENTRY_LENGTH);
            if (worldId == id) { // Found our palette id!
                return i;
            }
            
            if (worldId == 0) { // Previous one was last id
                // Allocate new palette id
                mem.writeVolatileInt(palette + i * PALETTE_ENTRY_LENGTH, id);
                return i;
            }
        }
        
        // If we get this far, palette is exhausted
        return -1; // Must change chunk type
    }
    
    /**
     * Gets world id for given palette id.
     * @param palette Palette to search the id from.
     * Note that it MUST be between 0 and 15 to avoid memory corruption.
     * @param paletteId Palette id.
     * @return World id.
     */
    private int getWorldId(@Pointer long palette, int paletteId) {
        return mem.readVolatileInt(palette + paletteId);
    }
    
    @Override
    public boolean convert(long from, long to, int type) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public OffheapChunk.Storage processQueries(OffheapChunk chunk, OffheapChunk.ChangeIterator changes) {
        long palette = chunk.memoryAddress(); // Palette is at beginning
        long blocks = palette + PALETTE_LENGTH;
        
        while (changes.hasNext()) {
            changes.next();
            int index = changes.getIndex();
            int id = changes.getBlockId();
            
            // Figure out correct palette id
            int paletteId = findPaletteId(palette, id);
            if (paletteId == -1) { // Palette has been exhausted
                // TODO handle failure
            }
            
            int byteIndex = byteIndex(index);
            int bitOffset = shiftOffset(index);
            
            long blockAddr = blocks + byteIndex;
            int value = mem.readVolatileByte(blockAddr);
            
            /* Clearing a half of byte without branches!
             * 
             * When no shifting is done (bitOffset==0), the first 4
             * bits are AND'd with 0xf and second 0x0, thus clearing
             * the second 4 bits.
             * 
             * When shifting is done by 4, first shift will rearrange
             * the byte in int in a way that the reverse is true.
             * first 4 bits and AND'd with 0x0 and second with 0x0.
             * The second shift will then arrange them back.
             */
            // Clear the part of byte where we will write
            value = value << bitOffset & 0xfffff0f0 >>> bitOffset;
            
            // Write the 4 byte palette id to first 4 bytes or last 4 bytes
            value = value | (paletteId << bitOffset);
            
            // Write our new value in place of old one
            // One block was changed, one not
            mem.writeVolatileByte(blockAddr, (byte) value);
        }
        
        return null;
    }

    @Override
    public int getChunkType() {
        // TODO Auto-generated method stub
        return 0;
    }
    
    public class Palette16BlockBuffer implements BlockBuffer {
        
        private final OffheapChunk chunk;
        private final MaterialRegistry registry;
        private int index;
        
        public Palette16BlockBuffer(OffheapChunk chunk) {
            this.chunk = chunk;
            this.registry = chunk.getWorldMaterialRegistry();
        }
        
        @Override
        public void close() throws Exception {
            // TODO buffer tracking in chunk
        }

        @Override
        public void seek(int index) {
            this.index = index;
        }

        @Override
        public void next() {
            index++;
        }

        @Override
        public boolean hasNext() {
            return index < DataConstants.CHUNK_MAX_BLOCKS;
        }

        @Override
        public void write(TerraMaterial material) {
            // TODO make direct writes work, chunk buffers are supposed to be FAST!
            chunk.queueChange(index, material.getWorldId());
        }

        @Override
        public TerraMaterial read() {
            try (OffheapChunk.Storage storage = chunk.getStorage()) {
                @Pointer long addr = storage.address;
                
                int byteIndex = byteIndex(index);
                int bitOffset = shiftOffset(index);

                byte data = mem.readVolatileByte(addr + PALETTE_LENGTH + byteIndex);
                int paletteId = data >>> bitOffset & 0xf;
                
                int worldId = getWorldId(addr, paletteId);
                return registry.getForWorldId(worldId);
            }
        }

        @Override
        public Object readRef() {
            return chunk.getRef(index);
        }

        @Override
        public void writeRef(Object ref) {
            chunk.setRef(index, ref);
        }
        
    }

    @Override
    public BlockBuffer createBuffer(OffheapChunk chunk) {
        return new Palette16BlockBuffer(chunk);
    }

}
