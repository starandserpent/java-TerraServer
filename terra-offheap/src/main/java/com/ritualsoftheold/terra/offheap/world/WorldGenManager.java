package com.ritualsoftheold.terra.offheap.world;

import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.data.DataHeuristics;
import com.ritualsoftheold.terra.offheap.data.WorldDataProvider;
import com.ritualsoftheold.terra.world.gen.WorldGenerator;

import it.unimi.dsi.fastutil.shorts.ShortArraySet;
import it.unimi.dsi.fastutil.shorts.ShortLinkedOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;

/**
 * Manages generating the world.
 *
 */
public class WorldGenManager {
    
    private WorldGenerator generator;
    
    /**
     * Some "clever" code to determine what data format to use.
     */
    private DataHeuristics heuristics;
    
    public void generate(float x, float y, float z, float scale) {
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
        WorldDataProvider provider = heuristics.getDataProvider(matCount, false);
    }
    
    /**
     * Calculates number of materials which are used in data.
     * Terribly inefficient, but sometimes world generators can't do better.
     * @param data
     * @return
     */
    private int getMaterialCount(short[] data) {
        ShortSet usedIds = new ShortLinkedOpenHashSet();
        for (int i = 0; i < data.length; i++) {
            usedIds.add(data[i]);
        }
        return usedIds.size();
    }
}
