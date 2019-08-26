package com.ritualsoftheold.terra.memory.data;

import com.ritualsoftheold.terra.core.BlockBuffer;

public interface BufferWithFormat extends BlockBuffer {
    
    WorldDataFormat getDataFormat();
}
