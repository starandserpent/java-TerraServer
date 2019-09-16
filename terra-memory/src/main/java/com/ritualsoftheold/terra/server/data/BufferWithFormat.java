package com.ritualsoftheold.terra.server.data;

import com.ritualsoftheold.terra.core.BlockBuffer;

public interface BufferWithFormat extends BlockBuffer {
    
    WorldDataFormat getDataFormat();
}
