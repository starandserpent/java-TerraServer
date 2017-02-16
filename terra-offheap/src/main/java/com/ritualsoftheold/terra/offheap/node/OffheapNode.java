package com.ritualsoftheold.terra.offheap.node;

import com.ritualsoftheold.terra.node.Node;
import com.ritualsoftheold.terra.offheap.data.OffheapObject;

public interface OffheapNode extends OffheapObject, Node {
    
    @Override
    default long l_getAddress() {
        return memoryAddress();
    }
}
