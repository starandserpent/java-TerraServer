package com.ritualsoftheold.terra.test;

import com.ritualsoftheold.terra.offheap.memory.MemoryUseListener;

public class DummyMemoryUseListener implements MemoryUseListener {

    @Override
    public void onAllocate(long amount) {
        
    }

    @Override
    public void onFree(long amount) {
        
    }

}
