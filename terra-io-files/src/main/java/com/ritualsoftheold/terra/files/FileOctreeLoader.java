package com.ritualsoftheold.terra.files;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.ritualsoftheold.terra.io.OctreeLoader;

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
    
    private static final Memory mem = OS.memory();

    private Path dir;
    private long fileSize;
    
    private Object[] locks;
    
    public FileOctreeLoader(Path dir, long fileSize) {
        this.dir = dir;
        this.fileSize = fileSize;
        this.locks = new Object[256];
        for (int i = 0; i < 256; i++) {
            locks[i] = new Object();
        }
    }
    
    @Override
    public long loadOctrees(int index, long address) {
        Path file = dir.resolve(index + ".terra");
        
        try {
            synchronized (locks[index]) {
                if (!Files.exists(file)) { // Create new file if necessary
                    RandomAccessFile f = new RandomAccessFile(file.toFile(), "rwd");
                    f.setLength(fileSize);
                    f.close();
                }
                long dataAddr = OS.map(FileChannel.open(file, StandardOpenOption.READ), MapMode.PRIVATE, 0, fileSize);
                long addr = mem.allocate(fileSize);
                mem.copyMemory(dataAddr, addr, fileSize);
                return addr;
            }
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    @Override
    public void saveOctrees(int index, long addr) {
        synchronized (locks[index]) {
            Path file = dir.resolve(index + ".terra");
            try {
                long mappedAddr = OS.map(FileChannel.open(file, StandardOpenOption.WRITE, StandardOpenOption.CREATE), MapMode.READ_WRITE, 0, fileSize);
                mem.copyMemory(addr, mappedAddr, fileSize); // Copy data to file
                OS.unmap(mappedAddr, fileSize);
            } catch (IOException e) {
                throw new IORuntimeException(e);
            }
        }
    }

}
