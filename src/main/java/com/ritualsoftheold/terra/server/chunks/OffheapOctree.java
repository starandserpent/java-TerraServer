package com.ritualsoftheold.terra.server.chunks;

import com.ritualsoftheold.terra.core.octrees.OctreeBase;
import com.ritualsoftheold.terra.core.octrees.OctreeNode;

import java.util.*;

public class OffheapOctree {

    private OctreeNode rootNode = null;
    private ArrayList<OctreeBase> octree;

    private int x, y, z;
    private int size;

    public OffheapOctree() {
        octree = new ArrayList<>();
    }

    public void setOctreeOrigin(int x, int y, int z, int size) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.size = size;
    }

    //We create the octree from a bottom up approach
    public void createOctree(OctreeBase[] leafNodes) {

        byte childCounter = 0;
        int nodeCounter = 0;
        Queue<OctreeNode> nodeQueue = new ArrayDeque<>();
        while (nodeCounter < leafNodes.length) {
            if (childCounter < 8) {
                octree.add(leafNodes[nodeCounter]);
                childCounter += 1;
                nodeCounter += 1;
            } else {
                OctreeBase c1 = octree.get(nodeCounter - 1);
                OctreeBase c2 = octree.get(nodeCounter - 2);
                OctreeBase c3 = octree.get(nodeCounter - 3);
                OctreeBase c4 = octree.get(nodeCounter - 4);
                OctreeBase c5 = octree.get(nodeCounter - 5);
                OctreeBase c6 = octree.get(nodeCounter - 6);
                OctreeBase c7 = octree.get(nodeCounter - 7);
                OctreeBase c8 = octree.get(nodeCounter - 8);

                OctreeNode parent = new OctreeNode(false);
                parent.setChildren(c1, c2, c3, c4, c5, c6, c7, c8);
                childCounter = 0;
                nodeQueue.add(parent);
            }
        }
        System.out.println("Length of arr: " + leafNodes.length + " Queue size: " + nodeQueue.size() + "  Size of array " + octree.size());

        while (!nodeQueue.isEmpty()) {
            if (childCounter < 8) {
                OctreeNode node = nodeQueue.poll();
                octree.add(node);
                childCounter += 1;
                nodeCounter += 1;
            } else {
                OctreeBase c1 = octree.get(nodeCounter - 1);
                OctreeBase c2 = octree.get(nodeCounter - 2);
                OctreeBase c3 = octree.get(nodeCounter - 3);
                OctreeBase c4 = octree.get(nodeCounter - 4);
                OctreeBase c5 = octree.get(nodeCounter - 5);
                OctreeBase c6 = octree.get(nodeCounter - 6);
                OctreeBase c7 = octree.get(nodeCounter - 7);
                OctreeBase c8 = octree.get(nodeCounter - 8);

                OctreeNode parent = new OctreeNode(false);
                parent.setChildren(c1, c2, c3, c4, c5, c6, c7, c8);
                childCounter = 0;
                nodeQueue.add(parent);
            }
        }
        OctreeBase c1 = octree.get(nodeCounter - 1);
        OctreeBase c2 = octree.get(nodeCounter - 2);
        OctreeBase c3 = octree.get(nodeCounter - 3);
        OctreeBase c4 = octree.get(nodeCounter - 4);
        OctreeBase c5 = octree.get(nodeCounter - 5);
        OctreeBase c6 = octree.get(nodeCounter - 6);
        OctreeBase c7 = octree.get(nodeCounter - 7);
        OctreeBase c8 = octree.get(nodeCounter - 8);
        rootNode = new OctreeNode(false);
        rootNode.setChildren(c1, c2, c3, c4, c5, c6, c7, c8);
        octree.add(rootNode);

        System.out.println("Length of arr: " + octree.size());
    }

    public ArrayList<OctreeBase> getOctreeNodes() {
        return octree;
    }
}
