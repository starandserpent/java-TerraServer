package com.ritualsoftheold.terra.data;

import com.ritualsoftheold.terra.buffer.BlockBuffer;

public interface BufferWithFormat extends BlockBuffer {
    
    WorldDataFormat getDataFormat();
}
