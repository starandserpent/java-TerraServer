package com.ritualsoftheold.terra.node;

public interface ChunkIterator {
    
    /**
     * Gets current 1D "distance" from chunk start.
     * @return Distance.
     */
    float getDistance();
    
    float getXCoord();
    
    float getYCoord();
    
    float getZCoord();
    
    /**
     * Gets a block and advances the iterator by one. Note that returned block
     * might be pooled by this iterator, and thus should not be stored.
     * @return Next block.
     */
    Block next();
    
    /**
     * Gets a block at this position. Note that this returns a block which is
     * never pooled, so it can be used externally.
     * @return This block, not pooled.
     */
    Block getBlock();
}
