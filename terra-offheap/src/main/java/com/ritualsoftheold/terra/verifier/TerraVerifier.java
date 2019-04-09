package com.ritualsoftheold.terra.verifier;

import com.ritualsoftheold.terra.DataConstants;
import com.ritualsoftheold.terra.chunk.compress.UncompressedChunkFormat;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Verifies correctness of Terra's world data. This is usually unnecessary on
 * server side, but somewhat CRITICAL on client side, at least if the server is
 * potentially not trustworthy.
 *
 */
// FIXME review security with new chunk buffer stuff!
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
     * @param address Address to octree data.
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
                    verifyChunkId(node);
                } // Else: need not verify: material id
            }
        } else { // Children are octrees or single nodes
            for (int i = 0; i < 8; i++) {
                if ((flags >>> i & 1) == 1) { // Must verify: octree
                    int node = mem.readVolatileInt(dataAddr + i * DataConstants.OCTREE_NODE_SIZE);
                    verifyOctreeId(node);
                } // Else: need not verify: material id
            }
        }
    }
    
    /**
     * Verifies that a the given chunk id is safe. If it is not,
     * a {@link VerifyFailedError} is thrown.
     * @param id Full chunk id.
     */
    public void verifyChunkId(int id) {
        int bufId = id >>> 16;
        int chunkId = id & 0xffff;
        
        // Potential over or underflows
        if (bufId < 0 || bufId >= maxChunkBuffers) {
            throw new VerifyFailedError("chunkptr, bufId: " + bufId);
        }
        if (chunkId < 0 || chunkId >= chunkBufferSize) {
            throw new VerifyFailedError("chunkptr, id: " + chunkId);
        }
    }
    
    /**
     * Verifies that a the given octree id is safe. If it is not,
     * a {@link VerifyFailedError} is thrown.
     * @param id Full octree id.
     */
    public void verifyOctreeId(int id) {
        int groupId = id >>> 24;
        int octreeId = id & 0xffffff;
        
        // Potential over or underflows
        if (groupId < 0 || groupId >= 256) {
            throw new VerifyFailedError("octreeptr, groupId: " + groupId);
        }
        if (octreeId < 0 || octreeId >= octreeBlockSize) {
            throw new VerifyFailedError("octreeptr, id: " + octreeId);
        }
    }

    public void verifyChunkLength(int length) {
        // TODO verify based on type
        if (length > UncompressedChunkFormat.INSTANCE.newDataLength()) {
            throw new VerifyFailedError("chunk length over max: " + length);
        }
    }
}
