package com.ritualsoftheold.terra.offheap.chunk.compress;

import com.ritualsoftheold.terra.buffer.BlockBuffer;
import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.material.TerraMaterial;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.MemoryArea;
import com.ritualsoftheold.terra.offheap.chunk.ChunkType;
import com.ritualsoftheold.terra.offheap.chunk.TooManyMaterialsException;
import com.ritualsoftheold.terra.offheap.data.BufferWithFormat;
import com.ritualsoftheold.terra.offheap.data.CriticalBlockBuffer;
import com.ritualsoftheold.terra.offheap.data.WorldDataFormat;
import com.ritualsoftheold.terra.offheap.memory.MemoryAllocator;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk.Storage;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * 4 bits per block. Max 16 block types in a chunk.
 *
 */
public class Palette16ChunkFormat implements ChunkFormat {
    
    public static final Palette16ChunkFormat INSTANCE = new Palette16ChunkFormat();
    
    private static final int PALETTE_SIZE = 16;
    
    private static final int PALETTE_ENTRY_LENGTH = 4;
    
    private static final int PALETTE_LENGTH = PALETTE_SIZE * PALETTE_ENTRY_LENGTH;
    
    private static final int CHUNK_LENGTH = DataConstants.CHUNK_MAX_BLOCKS / 2;
    
    private static final Memory mem = OS.memory();
    
    /**
     * Gets index of the byte where given index
     * @param index
     * @return
     */
    public static int byteIndex(int index) {
        return index >> 1; // Quick integer divide positive by 2
    }
    
    /**
     * Gets the bit offset in the byte towards which the value should be
     * shifted from right to left.
     * @param index
     * @return
     */
    public static int shiftOffset(int index) {
        // Quick modulo positive by 2
        // Get shift offset by reducing (0 -> -1, 1 -> 0)
        // and then multiplying by -4 to get shift offset in bits, positive
        return ((index & 1) - 1) * -4;
    }
    
    /**
     * Finds or create a palette id.
     * @param palette Memory area, which starts with palette definitions.
     * @param id World block id.
     * @return Palette id or -1 if palette is exhausted.
     */
    public static int findPaletteId(MemoryArea palette, int id) {
        for (int i = 0; i < PALETTE_LENGTH; i++) {
            int worldId = palette.readVolatileInt(i * PALETTE_ENTRY_LENGTH);
            if (worldId == id) { // Found our palette id!
                return i;
            }
            
            if (worldId == 0) { // Previous one was last id
                // Allocate new palette id
                palette.writeVolatileInt(i * PALETTE_ENTRY_LENGTH, id);
                return i;
            }
        }
        
        // If we get this far, palette is exhausted
        return -1; // Must change chunk type
    }
    
    /**
     * Gets world id for given palette id.
     * @param palette Palette to search the id from.
     * @param paletteId Palette id (0-15).
     * @return World id.
     */
    public static int getWorldId(MemoryArea palette, int paletteId) {
        return palette.readVolatileInt(paletteId * PALETTE_ENTRY_LENGTH);
    }
    
    /**
     * Reads world id of a block with given index. Internally, this is done
     * by reading the palette id, then getting the world id for it.
     * @param area Memory area that has chunk data.
     * @param index Block index.
     * @return World id.
     */
    public static int readWorldId(MemoryArea area, int index) {
        int byteIndex = byteIndex(index);
        int bitOffset = shiftOffset(index);
        int blockIndex = PALETTE_LENGTH + byteIndex;

        byte data = area.readVolatileByte(blockIndex);
        int paletteId = (data >>> bitOffset) & 0xf;
        
        return getWorldId(area, paletteId);
    }
    
    /**
     * Writes a world id for the block with given index.
     * @param area Memory area with chunk data.
     * @param index Block index.
     * @param id New block id.
     * @return True, if the write succeeded. False, if the palette has
     * been exhausted and this chunk format needs to be replaced.
     */
    public boolean writeWorldId(MemoryArea area, int index, int id) {
        // Palette is at start, blocks come immediately after it
        int blocksOffset = PALETTE_LENGTH;
        
        // Figure out correct palette id
        int paletteId = findPaletteId(area, id);
        if (paletteId == -1) { // Palette has been exhausted
            return false;
        }
        
        int byteIndex = byteIndex(index);
        int bitOffset = shiftOffset(index);
        
        int blockIndex = blocksOffset + byteIndex;
        int value = area.readVolatileByte(blockIndex);
        
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
        value = ((value << bitOffset) & 0xfffff0f0) >>> bitOffset;
        
        // Write the 4 byte palette id to first 4 bytes or last 4 bytes
        value = value | (paletteId << bitOffset);
        
        // Write our new value in place of old one
        // One block was changed, one not
        area.writeVolatileByte(blockIndex, (byte) value);
        return true;
    }
    
    @Override
    public Storage convert(Storage origin, ChunkFormat format, MemoryAllocator allocator) {
        if (format == UncompressedChunkFormat.INSTANCE) {
            long addr = allocator.allocate(DataConstants.CHUNK_MAX_BLOCKS * 4);
            long blocksOffset = PALETTE_LENGTH;
            
            int offset = 0;
            for (int i = 0; i < CHUNK_LENGTH; i++) {
                byte values = origin.readVolatileByte(blocksOffset + i);
                int block1 = getWorldId(origin, values >>> 4);
                int block2 = getWorldId(origin, values & 0xf);
                
                mem.writeVolatileInt(addr + offset, block1);
                mem.writeVolatileInt(addr + offset + 4, block2);
                offset += 8;
            }
            
            return new Storage(format, addr, DataConstants.CHUNK_MAX_BLOCKS * 4);
        }
        
        return null; // Conversion not supported
    }

    @Override
    public OffheapChunk.Storage processQueries(OffheapChunk chunk, Storage storage, OffheapChunk.ChangeIterator changes) {
        while (changes.hasNext()) {
            changes.next();
            int index = changes.getIndex();
            int id = changes.getBlockId();
            
            boolean writeOk = writeWorldId(storage, index, id);
            if (!writeOk) { // Write failed, palette has been exhausted
                // Fallback to next best compressed format
                // FIXME don't fallback to uncompressed immediately
                changes.ignoreNext();
                return convert(storage, UncompressedChunkFormat.INSTANCE, chunk.getChunkBuffer().getAllocator());
            }
        }
        
        return null;
    }

    @Override
    public byte getChunkType() {
        return ChunkType.PALETTE16;
    }
    
    public class Palette16BlockBuffer implements BufferWithFormat {

        private MemoryArea area;
        private final OffheapChunk chunk;
        private final Storage storage;
        private final MaterialRegistry registry;
        private int index;
        
        public Palette16BlockBuffer(OffheapChunk chunk, Storage storage) {
            this.chunk = chunk;
            this.storage = storage;
            this.registry = chunk.getWorldMaterialRegistry();
        }
        
        @Override
        public void close() {
           storage.close();
        }

        @Override
        public void seek(int index) {
            this.index = index;
        }
        
        @Override
        public int position() {
            return index;
        }

        @Override
        public void next() {
            index++;
        }

        @Override
        public boolean hasNext() {
            return index < DataConstants.CHUNK_MAX_BLOCKS - 1;
        }

        @Override
        public void write(TerraMaterial material) {
            // TODO make direct writes work, chunk buffers are supposed to be FAST!
            chunk.queueChange(index, material.getWorldId());
        }

        @Override
        public TerraMaterial read() {
            int worldId = readWorldId(storage, index);
            System.out.println(worldId);
            System.out.println("hoj");
            return registry.getForWorldId(worldId);
        }

        @Override
        public Object readRef() {
            return chunk.getRef(index);
        }

        @Override
        public void writeRef(Object ref) {
            chunk.setRef(index, ref);
        }

        @Override
        public WorldDataFormat getDataFormat() {
            return INSTANCE;
        }
        
    }
    
    public class Palette16CriticalBuffer implements CriticalBlockBuffer {
        
        private final MemoryArea area;
        private final MaterialRegistry registry;
        private int index;
        
        public Palette16CriticalBuffer(MemoryArea area, MaterialRegistry registry) {
            this.area = area;
            this.registry = registry;
        }

        @Override
        public void close() {
            // Not needed
        }

        @Override
        public void seek(int index) {
            this.index = index;
        }

        @Override
        public int position() {
            return index;
        }

        @Override
        public void next() {
            index++;
            writeWorldId(area, index, 0);
       }

        @Override
        public boolean hasNext() {
            return index < DataConstants.CHUNK_MAX_BLOCKS - 1;
        }

        @Override
        public void write(TerraMaterial material) {
            boolean writeOk = writeWorldId(area, index, material.getWorldId());
            if (!writeOk) {
                throw new TooManyMaterialsException();
            }
        }

        @Override
        public TerraMaterial read() {
            int worldId = readWorldId(area, index);
            return registry.getForWorldId(worldId);
        }


        @Override
        public Object readRef() {
            // TODO critical buffer ref support
            throw new UnsupportedOperationException("TODO implement this");
        }

        @Override
        public void writeRef(Object ref) {
            // TODO critical buffer ref support
            throw new UnsupportedOperationException("TODO implement this");
        }

        @Override
        public WorldDataFormat getDataFormat() {
            return INSTANCE;
        }

        @Override
        public Storage getStorage() {
            return null; // TODO
        }
        
    }

    @Override
    public BufferWithFormat createBuffer(OffheapChunk chunk, Storage storage) {
        return new Palette16BlockBuffer(chunk, storage);
    }

    @Override
    public int newDataLength() {
        return PALETTE_LENGTH + CHUNK_LENGTH;
    }

    @Override
    public CriticalBlockBuffer createCriticalBuffer(Storage storage, MaterialRegistry materialRegistry) {
        return new Palette16CriticalBuffer(storage, materialRegistry);
    }

}
