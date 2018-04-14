package com.ritualsoftheold.terra.offheap.data;

import com.ritualsoftheold.terra.buffer.BlockBuffer;

public interface BufferWithFormat extends BlockBuffer {
    
    WorldDataFormat getDataFormat();
}
