package com.ritualsoftheold.terra.files;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.io.ChunkLoader;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;

public class FileChunkLoader implements ChunkLoader {
    
    private static final Memory mem = OS.memory();
    
    /**
     * How much extra stuff we can throw at beginning. Increasing this
     * will probably corrupt existing saves, so have a bit extra.
     */
    public static final int FILE_META_LENGTH = 16;

    private Path dir;
    
    public FileChunkLoader(Path dir) {
        this.dir = dir;
    }
    
    @Override
    public ChunkBuffer loadChunks(short index, ChunkBuffer buf) {
        Path file = dir.resolve(index + ".terrac");
        if (!Files.exists(file)) { // Error handling
            throw new IllegalArgumentException("cannot load non-existent chunks");
        }
        try {
            long len = Files.size(file);
            long addr = OS.map(FileChannel.open(file, StandardOpenOption.READ), MapMode.PRIVATE, 0, len); // Map to memory
            int chunkCount = mem.readInt(addr); // Write chunk count to metadata
            buf.load(addr + FILE_META_LENGTH, chunkCount); // Load chunks
            OS.unmap(addr, len); // Unmap data after it is not needed
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        
        return buf; // Return it - for chaining I guess
    }

    @Override
    public ChunkBuffer saveChunks(short index, ChunkBuffer buf) {
        Path file = dir.resolve(index + ".terrac");
        try {
            long len = buf.getSaveSize();
            long addr = OS.map(FileChannel.open(file, StandardOpenOption.WRITE, StandardOpenOption.CREATE), MapMode.READ_WRITE, 0, FILE_META_LENGTH + len); // Map to memory
            mem.writeInt(addr, buf.getChunkCount()); // Write chunk count to metadata
            buf.save(addr + FILE_META_LENGTH); // Save to memory mapped region
            OS.unmap(addr, len); // Unmap data after it is not needed
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        
        return buf;
    }

    @Override
    public int countBuffers() {
        return 0; // TODO implement this; need to have metadata or loop all files
    }

}
