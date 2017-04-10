package com.ritualsoftheold.terra.offheap.octree;

import java.util.concurrent.BlockingQueue;
import java.util.function.LongConsumer;

import com.ritualsoftheold.terra.offheap.io.OctreeLoader;

public class OctreeLoaderThread extends Thread {
    
    private BlockingQueue<GroupEntry> queue;
    
    private OctreeLoader loader;
    
    public OctreeLoaderThread(BlockingQueue<GroupEntry> loaderQueue,
            OctreeLoader loader) {
        this.queue = loaderQueue;
        this.loader = loader;
    }

    @Override
    public void run() {
        while (!this.isInterrupted()) {
            try {
                GroupEntry entry = queue.take(); // Take from queue - or wait here if nothing to do
                if (entry.save) {
                    loader.saveOctrees(entry.groupId, entry.address);
                } else {
                    loader.loadOctrees(entry.groupId, entry.address);
                }
                
                if (entry.callback != null) {
                    entry.callback.accept(entry.address); // Notify callback
                }
            } catch (InterruptedException e) {
                continue;
            }
        }
    }
    
    /**
     * Represents an octree group which needs to be loaded or saved.
     *
     */
    public static class GroupEntry {
        
        public GroupEntry(boolean save, byte id, long address, LongConsumer callback) {
            this.save = save;
            this.groupId = id;
            this.address = address;
            this.callback = callback;
        }
        
        /**
         * If this group requires saving. Otherwise, it is assumed that
         * it needs loading.
         */
        public boolean save;
        
        /**
         * Group id.
         */
        public byte groupId;
        
        /**
         * Address to where data is (when saving) or where it shall
         * be written (when loading).
         */
        public long address;
        
        /**
         * Called when operation is done, if exists.
         */
        public LongConsumer callback;
    }
}
