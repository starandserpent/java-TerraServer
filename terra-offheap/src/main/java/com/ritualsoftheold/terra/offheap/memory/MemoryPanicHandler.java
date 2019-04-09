package com.ritualsoftheold.terra.offheap.memory;

/**
 * When memory manager notices that too much memory is used or enough
 * memory cannot be freed, this is called and may decide how to continue.
 *
 */
public interface MemoryPanicHandler {
    
    /**
     * Called when goal to free memory is not met.
     * @param goal What was the goal.
     * @param possible How much can be freed if
     * {@link PanicResult#CONTINUE} is returned.
     * @return What the memory manager should do.
     */
    PanicResult goalNotMet(long goal, long possible);
    
    /**
     * Called when memory manager notices that it has exceeded maximum memory
     * amount assigned for it.
     * @param max Maximum amount.
     * @param used Used amount.
     * @param possible How much could be collected, if this error is ignored.
     * @return What the memory manager should do.
     */
    PanicResult outOfMemory(long max, long used, long possible);
    
    public enum PanicResult {
        
        /**
         * Tell the memory manager to do its best, and that is is ok if
         * goal was not met.
         */
        CONTINUE,
        
        /**
         * Ask memory manager to interrupt whatever it was doing.
         * It will not continue normal operation, but it will not panic either.
         */
        INTERRUPT,
    }
}
