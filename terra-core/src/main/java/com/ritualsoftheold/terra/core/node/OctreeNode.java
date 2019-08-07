package com.ritualsoftheold.terra.core.node;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesMarshallable;
import net.openhft.chronicle.values.Array;
import net.openhft.chronicle.values.Copyable;

import java.io.Serializable;

/*
*   New Octree definition. Basic Node for a linear octree
*
*
*/
public class OctreeNode {
    public int chunkLoc;
    public long locCode;
    public byte childExists;

    public OctreeNode[] children;

    public OctreeNode(boolean isLeaf){
        chunkLoc = 0;
        locCode = 0;
        childExists = 0;
        if(!isLeaf)
            children = new OctreeNode[]{null,null,null,null,null,null,null,null};
    }
    public OctreeNode(){
        this(true);
    }

//    public int getNodeDepth(){
//        int lcIterator = locCode;
//        int depth = 0;
//        while (lcIterator != 0){
//            lcIterator >>= 3;//Pop 3 off the stack
//            //We now know what the marker value is, and check if it is 0 or 1;
//            //the locCode packed in 4 byte intervals
//            // [1][000]
//            // 1 bit end marker, 3 bit child marker
//            // The 1 bit end marker indicates that
//            depth+=1;
//        }
//        return depth;
//    }

}
