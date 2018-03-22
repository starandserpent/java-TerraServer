package com.ritualsoftheold.terra.offheap.verifier;

import com.ritualsoftheold.terra.offheap.DataConstants;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Verifies correctness of Terra's world data. This is usually unnecessary on
 * server side, but somewhat CRITICAL on client side, at least if the server is
 * potentially not trustworthy.
 *
 */
public class TerraVerifier {
    
    private static final Memory mem = OS.memory();
    
    /**
     * Octree id must be this minus one at most.
     */
    private int octreeBlockSize;
    
    /**
     * Chunk index in buffer must be this minus one at most.
     */
    private int chunkBufferSize;
    
    /**
     * Chunk buffer id must be this minus one at most.
     */
    private int maxChunkBuffers;
    
    /**
     * Initializes a new verifier with given limits.
     * @param octreeBlockSize Octree block size, aka how many octrees can
     * fit to a single octree group.
     * @param chunkBufferSize Chunk buffer size.
     * @param maxChunkBuffers Maximum amount of chunk buffers.
     */
    public TerraVerifier(int octreeBlockSize, int chunkBufferSize, int maxChunkBuffers) {
        this.octreeBlockSize = octreeBlockSize;
        this.chunkBufferSize = chunkBufferSize;
        this.maxChunkBuffers = maxChunkBuffers;
    }
    
    /**
     * Verified given octree. If it contains unsafe data, a
     * {@link VerifyFailedError} is thrown.
     * @param addr Address to octree data.
     * @param subChunks If octrees children could be chunks
     * (usually scale ~16 meters).
     */
    public void verifyOctree(long addr, boolean subChunks) {
        byte flags = mem.readVolatileByte(addr);
        long dataAddr = addr + 1;
        
        if (subChunks) { // Children may be chunks
            for (int i = 0; i < 8; i++) {
                if ((flags >>> i & 1) == 1) { // Must verify: chunk
                    int node = mem.readVolatileInt(dataAddr + i * DataConstants.OCTREE_NODE_SIZE);
                    int bufId = node >>> 16;
                    int chunkId = node & 0xffff;
                    
                    // Potential over or underflows
                    if (bufId < 0 || bufId >= maxChunkBuffers) {
                        throw new VerifyFailedError("chunkptr, bufId: " + bufId);
                    }
                    if (chunkId < 0 || chunkId >= chunkBufferSize) {
                        throw new VerifyFailedError("chunkptr, id: " + chunkId);
                    }
                } // Else: need not verify: material id
            }
        } else { // Children are octrees or single nodes
            for (int i = 0; i < 8; i++) {
                if ((flags >>> i & 1) == 1) { // Must verify: octree
                    int node = mem.readVolatileInt(dataAddr + i * DataConstants.OCTREE_NODE_SIZE);
                    int groupId = node >>> 24;
                    int octreeId = node & 0xffffff;
                    
                    // Potential over or underflows
                    if (groupId < 0 || groupId >= 256) {
                        throw new VerifyFailedError("octreeptr, groupId: " + groupId);
                    }
                    if (octreeId < 0 || octreeId >= octreeBlockSize) {
                        throw new VerifyFailedError("octreeptr, id: " + octreeId);
                    }
                } // Else: need not verify: material id
            }
        }
    }
}
