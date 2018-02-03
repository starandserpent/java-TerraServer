package com.ritualsoftheold.terra.mesher;

import io.netty.buffer.ByteBuf;

/**
 * TODO a mesh container class to share code between meshers.
 *
 */
public class MeshContainer {
    
    private ByteBuf vertices;
    
    private ByteBuf indices;
    
    private ByteBuf texCoords;
}
