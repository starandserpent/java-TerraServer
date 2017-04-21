package com.ritualsoftheold.terra.files;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.ritualsoftheold.terra.offheap.io.OctreeLoader;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;

/**
 * Loads octrees from separate files in one directory.
 * Holds persistent, private memory mappings.
 * 
 * Note that the files are loaded in private mode, thus
 * no automated saving is done.
 * 
 *
 */
public class FileOctreeLoader implements OctreeLoader {
    
    private Memory mem = OS.memory();

    private Path dir;
    private long fileSize;
    
    public FileOctreeLoader(Path dir, long fileSize) {
        this.dir = dir;
        this.fileSize = fileSize;
    }
    
    @Override
    public long loadOctrees(byte index, long address) {
        Path file = dir.resolve(index + ".octree");
        if (!Files.exists(file)) { // Error handling
            throw new IllegalArgumentException("cannot load non-existent octree");
        }
        
        try {
            // TODO implement a way to unload previous stuff we have mapped if loadOctrees method is misused
            return OS.map(FileChannel.open(file, StandardOpenOption.READ), MapMode.PRIVATE, 0, fileSize);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    @Override
    public void saveOctrees(byte index, long addr) {
        Path file = dir.resolve(index + ".octree");
        try {
            long mappedAddr = OS.map(FileChannel.open(file, StandardOpenOption.WRITE, StandardOpenOption.CREATE), MapMode.READ_WRITE, 0, fileSize);
            mem.copyMemory(addr, mappedAddr, fileSize); // Copy data to file
            OS.unmap(mappedAddr, fileSize);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

}
