package com.ritualsoftheold.terra.core.node;

import java.io.Serializable;

/*
*   New Octree definition. Basic Node for a linear octree
*
*
*/
public class OctreeNode implements Serializable {
    public int chunkLoc;
    public int locCode;
    public byte childExists;

    public OctreeNode(){
        chunkLoc = 0;
        locCode = 0;
        childExists = 0;
    }

    public int getNodeDepth(){
        int lcIterator = locCode;
        int depth = 0;
        while (lcIterator != 0){
            lcIterator >>= 3;//Pop 3 off the stack
            //We now know what the marker value is, and check if it is 0 or 1;
            //the locCode packed in 4 byte intervals
            // [1][000]
            // 1 bit end marker, 3 bit child marker
            // The 1 bit end marker indicates that
            depth+=1;
        }
        return depth;
    }

}
