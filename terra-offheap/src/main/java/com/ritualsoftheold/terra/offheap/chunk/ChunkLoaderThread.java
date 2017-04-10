package com.ritualsoftheold.terra.offheap.chunk;

import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

import com.ritualsoftheold.terra.offheap.io.ChunkLoader;

/**
 * Handles loading chunks without using blocking operations.
 *
 */
public class ChunkLoaderThread extends Thread {
    
    /**
     * Queue of buffers which need saving or loading.
     */
    private BlockingQueue<BufferEntry> queue;
    
    private ChunkLoader loader;
    
    public ChunkLoaderThread(BlockingQueue<BufferEntry> queue, ChunkLoader loader) {
        this.queue = queue;
        this.loader = loader;
    }
    
    @Override
    public void run() {
        while (!this.isInterrupted()) {
            try {
                BufferEntry entry = queue.take(); // Take from queue - or wait here if nothing to do
                if (entry.save) {
                    loader.saveChunks(entry.bufferId, entry.buffer);
                } else {
                    loader.loadChunks(entry.bufferId, entry.buffer);
                }
                
                if (entry.callback != null) {
                    entry.callback.accept(entry.buffer); // Notify callback
                }
            } catch (InterruptedException e) {
                continue;
            }
        }
    }
    
    /**
     * Represents a buffer which needs to be saved or loaded with this thread.
     *
     */
    public static class BufferEntry {
        
        public BufferEntry(boolean save, short id, ChunkBuffer buf, Consumer<ChunkBuffer> callback) {
            this.save = save;
            this.bufferId = id;
            this.buffer = buf;
            this.callback = callback;
        }
        
        /**
         * If this buffer requires saving. Otherwise, it is assumed that
         * it needs loading.
         */
        public boolean save;
        
        /**
         * Buffer id.
         */
        public short bufferId;
        
        /**
         * The actual buffer.
         */
        public ChunkBuffer buffer;
        
        /**
         * Called when operation is done, if exists.
         */
        public Consumer<ChunkBuffer> callback;
    }
}
