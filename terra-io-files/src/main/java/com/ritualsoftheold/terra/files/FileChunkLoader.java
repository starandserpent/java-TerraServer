package com.ritualsoftheold.terra.files;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.io.ChunkLoader;

import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;

// TODO waiting for ChunkBuffer improvements
public class FileChunkLoader implements ChunkLoader {

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
            buf.load(addr); // Load chunks
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
            long addr = OS.map(FileChannel.open(file, StandardOpenOption.WRITE, StandardOpenOption.CREATE), MapMode.READ_WRITE, 0, len); // Map to memory
            buf.save(addr); // Save to memory mapped region
            OS.unmap(addr, len); // Unmap data after it is not needed
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        
        return buf;
    }

}
