package com.ritualsoftheold.terra.core.node;

/*
*   New Octree definition. Basic Node for a linear octree
*/
public class OctreeNode implements OctreeBase {
    private float worldX;
    private float worldY;
    private float worldZ;

    private int halfSize = 0;

    private OctreeBase[] children;

    public OctreeNode(boolean isLeaf){
        children = new OctreeBase[]{null,null,null,null,null,null,null,null};
    }

    public OctreeNode(){
        this(true);
    }

    public void setChildren(OctreeBase c1,OctreeBase c2,OctreeBase c3,OctreeBase c4,OctreeBase c5,OctreeBase c6,OctreeBase c7,OctreeBase c8){
        this.children[0]=c1;
        this.children[1]=c2;
        this.children[2]=c3;
        this.children[3]=c4;
        this.children[4]=c5;
        this.children[5]=c6;
        this.children[6]=c7;
        this.children[7]=c8;
        calculateCenter();
    }

    private void calculateCenter() {
//        if(children[0] instanceof  OctreeLeaf){
//            OctreeLeaf leaf = (OctreeLeaf)children[0];
//                this.worldX = leaf.worldX -4;
//                this.worldY =leaf.worldY -4;
//                this.worldZ = leaf.worldZ -4;
//        }else{
//            OctreeNode child = (OctreeNode)children[0];
//                this.worldX = child.worldX-child.halfSize;
//                this.worldY = child.worldY-child.halfSize;
//                this.worldZ = child.worldZ-child.halfSize;
//        }

        for (int i = 0; i < 8; i++) {

            if (children[i] instanceof OctreeLeaf) {
                OctreeLeaf leaf = (OctreeLeaf) children[i];
                this.worldX += leaf.worldX;
                this.worldY += leaf.worldY;
                this.worldZ += leaf.worldZ;
                halfSize += 4;
            } else {
                System.out.println("Test");
                OctreeNode child = (OctreeNode) children[i];
                this.worldX += child.worldX;
                this.worldY += child.worldY;
                this.worldZ += child.worldZ;
                int halfSizemore = child.halfSize >> 1;
                halfSize += halfSizemore;
            }
        }

        halfSize = halfSize / 2;
        this.worldX /= 8;
        this.worldY /= 8;
        this.worldZ /= 8;
        //Divide by 2 using bitshift
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
