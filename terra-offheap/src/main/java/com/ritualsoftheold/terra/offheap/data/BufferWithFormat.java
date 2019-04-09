package com.ritualsoftheold.terra.offheap.data;

import com.ritualsoftheold.terra.core.buffer.BlockBuffer;

public interface BufferWithFormat extends BlockBuffer {
    
    WorldDataFormat getDataFormat();
}
