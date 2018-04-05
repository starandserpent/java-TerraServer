package com.ritualsoftheold.terra.offheap.world;

import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.chunk.ChunkStorage;
import com.ritualsoftheold.terra.offheap.chunk.compress.ChunkFormat;
import com.ritualsoftheold.terra.offheap.data.DataHeuristics;
import com.ritualsoftheold.terra.offheap.data.WorldDataFormat;
import com.ritualsoftheold.terra.offheap.octree.OctreeNodeFormat;
import com.ritualsoftheold.terra.world.gen.WorldGenerator;

import it.unimi.dsi.fastutil.shorts.ShortLinkedOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Manages generating the world using an underlying world generator.
 * This way world generators don't have to deal with Terra's internals.
 *
 */
public class WorldGenManager {
    
    private static final Memory mem = OS.memory();
    
    /**
     * World generator which does the actual hard work:
     * fills arrays after each other with blocks.
     */
    private WorldGenerator generator;
    
    /**
     * Some "clever" code to determine what data format to use.
     */
    private DataHeuristics heuristics;
    
    private ChunkStorage chunkStorage;
    
    public WorldGenManager(WorldGenerator generator, DataHeuristics heuristics, ChunkStorage chunkStorage) {
        this.generator = generator;
        this.heuristics = heuristics;
        this.chunkStorage = chunkStorage;
    }
    
    /**
     * Generates a piece of world at given coordinates with given scale.
     * The parent octree will be modified to include reference to
     * data once generation is complete.
     * @param address Address of parent octree.
     * @param index Index of the piece in its parent octree.
     * @param x X coordinate of center.
     * @param y Y coordinate of center.
     * @param z Z coordinate of center.
     * @param scale Scale.
     */
    public void generate(long addr, int index, float x, float y, float z, float scale) {
        short[] data = new short[DataConstants.CHUNK_MAX_BLOCKS];
        WorldGenerator.Metadata meta = new WorldGenerator.Metadata();
        
        // Delegate to actual world generator
        generator.generate(data, x, y, z, scale, meta);
        
        // Calculate material count if needed
        int matCount = meta.materialCount;
        if (matCount == -1) {
            matCount = getMaterialCount(data);
        }
        
        // Get data provider which best suits for this piece of generated world
        WorldDataFormat format = heuristics.getDataFormat(matCount);
        
        // Handle octrees and chunk separately...
        if (format.isOctree()) {
           // It is just a single octree node
            ((OctreeNodeFormat) format).modifyFlag(addr, index, 0); // Never use it as chunk pointer
            ((OctreeNodeFormat) format).setNode(addr, index, data[0]); // Set actual data there
            // TODO fix potential race conditions
        } else {
            int id = chunkStorage.newChunk();
            if (!mem.compareAndSwapInt(addr + index * 4, 0, id)) {
                // Someone got there before us!
                return; // -> they get to do this
            }
            
            ChunkFormat chunkFormat = (ChunkFormat) format;
            ChunkBuffer buf = chunkStorage.getBuffer(id >>> 16);
            // Allocate memory and move data there
            ChunkFormat.SetAllResult result = chunkFormat.setAllBlocks(data, buf.getAllocator());
            // Get chunk index inside buffer and configure chunk with it
            int bufIndex = id & 0xffff;
            buf.setChunkAddr(bufIndex, result.addr);
            buf.setChunkLength(index, result.length);
            int type = result.typeSwap;
            if (type == -1) {
                type = chunkFormat.getChunkType();
            }
            buf.setChunkType(bufIndex, (byte) chunkFormat.getChunkType()); // Type is set last for a good reason
            // Empty chunk will block most calls until it is no longer empty
            // (so we can safely mess with address and length before we set type)
        }
        
    }
    
    /**
     * Calculates number of materials which are used in data.
     * Terribly inefficient, but sometimes world generators can't do better.
     * @param data World data as short array.
     * @return Number of different materials used.
     */
    private int getMaterialCount(short[] data) {
        ShortSet usedIds = new ShortLinkedOpenHashSet();
        for (int i = 0; i < data.length; i++) {
            usedIds.add(data[i]);
        }
        return usedIds.size();
    }
}
