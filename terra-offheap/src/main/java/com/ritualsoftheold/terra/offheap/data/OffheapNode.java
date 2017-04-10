package com.ritualsoftheold.terra.offheap.data;

import com.ritualsoftheold.terra.node.Node;

public interface OffheapNode extends OffheapObject, Node {
    
    @Override
    default long l_getAddress() {
        return memoryAddress();
    }
    
    default void requireValid(String message) {
        if (!isValid()) {
            throw new IllegalStateException(message);
        }
    }
    
    default void requireValid() {
        requireValid("invalid node (address missing)");
    }
}
