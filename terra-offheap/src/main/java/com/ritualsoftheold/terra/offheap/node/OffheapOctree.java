package com.ritualsoftheold.terra.offheap.node;

import com.ritualsoftheold.terra.core.buffer.BlockBuffer;
import com.ritualsoftheold.terra.core.material.MaterialRegistry;
import com.ritualsoftheold.terra.core.material.TerraMaterial;
import com.ritualsoftheold.terra.core.node.*;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.ChunkLArray;
import com.ritualsoftheold.terra.offheap.data.OffheapNode;

import net.openhft.chronicle.bytes.NativeBytesStore;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import net.openhft.chronicle.values.Values;

import java.util.*;

// TODO implement this class or figure out something better
public class OffheapOctree implements Octree, OffheapNode {
    
    private static final Memory mem = OS.memory();
    
    /**
     * Memory address of the octree.
     */
    private long addr;

    private int octreeId;
    
    private MaterialRegistry materialRegistry;

    private OctreeNode rootNode = null;
    private ArrayList<OctreeBase> octree;

    private int x,y,z;
    private int size;


    public OffheapOctree(long addr, int octreeId, MaterialRegistry materialRegistry) {
        this.addr = addr;
        this.octreeId = octreeId;
        this.materialRegistry = materialRegistry;
    }
    public OffheapOctree(MaterialRegistry materialRegistry){
        this.materialRegistry = materialRegistry;
        octree = new ArrayList<>();
//        IntValue avgKeyTmp = Values.newHeapInstance(IntValue.class);
//        avgKeyTmp.setValue(Integer.MAX_VALUE);
//        linearOctree = new HashMap<>();
//        OctreeNode root = new OctreeNode();
//        linearOctree.put(root.locCode,root);
//        Point offHeapPoint = Values.newNativeReference(Point.class);
//        long size = offHeap159632asdf
//        Point.maxSize();
//        NativeBytesStore<Void> offHeapStore = NativeBytesStore.nativeStore(size);
//        offHeapPoint.bytesStore(offHeapStore,0,size);
//        offHeapPoint.setX(0);
//        offHeapPoint.setY(0);
//        System.out.println("Value: "+offHeapPoint);
//        offHeapStore.release();
    }
    //--------------------------------------
    // NEW LINEAR OCTREE METHODS
    //--------------------------------------
    public void SetOctreeOrigin(int x,int y, int z, int size){
        this.x = x;
        this.y = y;
        this.z = z;
        this.size = size;
    }
    public OctreeNode GetParentNode(){
        return rootNode;
    }
    //We create the octree from a bottom up approach
    public void createOctree(OctreeBase[] leafNodes){
        byte childCounter = 0;
        int nodeCounter = 0;
        Queue<OctreeNode> nodeQueue = new ArrayDeque();
        while(nodeCounter < leafNodes.length){
            if(childCounter < 8){
                octree.add(leafNodes[nodeCounter]);
                childCounter+=1;
                nodeCounter+=1;
            }
            else{
                OctreeBase c1 = octree.get(nodeCounter-1);
                OctreeBase c2 =  octree.get(nodeCounter-2);
                OctreeBase c3 =  octree.get(nodeCounter-3);
                OctreeBase c4 =  octree.get(nodeCounter-4);
                OctreeBase c5 =  octree.get(nodeCounter-5);
                OctreeBase c6 =  octree.get(nodeCounter-6);
                OctreeBase c7 =  octree.get(nodeCounter-7);
                OctreeBase c8 =  octree.get(nodeCounter-8);

                OctreeNode parent = new OctreeNode(false);
                parent.setChildren(c1,c2,c3,c4,c5,c6,c7,c8);
                childCounter=0;
                nodeQueue.add(parent);
            }
        }
        System.out.println("Length of arr: "+leafNodes.length+" Queue size: "+nodeQueue.size()+"  Size of array "+octree.size());
        while(!nodeQueue.isEmpty()){
            if(childCounter < 8){
                OctreeNode node =(OctreeNode) nodeQueue.poll();
                octree.add(node);
                childCounter+=1;
                nodeCounter+=1;
            }else{
                OctreeBase c1 = octree.get(nodeCounter-1);
                OctreeBase c2 =  octree.get(nodeCounter-2);
                OctreeBase c3 =  octree.get(nodeCounter-3);
                OctreeBase c4 =  octree.get(nodeCounter-4);
                OctreeBase c5 =  octree.get(nodeCounter-5);
                OctreeBase c6 =  octree.get(nodeCounter-6);
                OctreeBase c7 =  octree.get(nodeCounter-7);
                OctreeBase c8 =  octree.get(nodeCounter-8);

                OctreeNode parent = new OctreeNode(false);
                parent.setChildren(c1,c2,c3,c4,c5,c6,c7,c8);
                childCounter=0;
                nodeQueue.add(parent);
            }

        }
        OctreeBase c1 = octree.get(nodeCounter-1);
        OctreeBase c2 =  octree.get(nodeCounter-2);
        OctreeBase c3 =  octree.get(nodeCounter-3);
        OctreeBase c4 =  octree.get(nodeCounter-4);
        OctreeBase c5 =  octree.get(nodeCounter-5);
        OctreeBase c6 =  octree.get(nodeCounter-6);
        OctreeBase c7 =  octree.get(nodeCounter-7);
        OctreeBase c8 =  octree.get(nodeCounter-8);
        rootNode =  new OctreeNode(false);
        rootNode.setChildren(c1,c2,c3,c4,c5,c6,c7,c8);
        octree.add(rootNode);

        System.out.println("Length of arr: "+octree.size());
    }
    public ArrayList<OctreeBase> getOctreeNodes(){return octree;}
    //--------------------------------------
    @Override
    public Type getNodeType() {
        return Type.OCTREE;
    }

    @Override
    public Node getNodeAt(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Octree getOctreeAt(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Chunk getChunkAt(int index) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public TerraMaterial getBlockAt(int index) throws ClassCastException {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public void setNodeAt(int index, Node node) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Node[] getNodes() {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public void setNodes(Node[] nodes) {
        // TODO Auto-generated method stub
        
    }

    public long getNodeAddr(int index) {
        // Address + metadata + size of node * index
        return addr + 1 + DataConstants.OCTREE_NODE_SIZE * index;
    }

    public int getNodeData(int index) {
        return mem.readInt(getNodeAddr(index));
    }

    @Override
    public long memoryAddress() {
        return addr;
    }
    
    @Override
    public int memoryLength() {
        return DataConstants.OCTREE_SIZE;
    }

    @Override
    public BlockBuffer getBuffer() {
        return null; // Buffers for octrees would be pretty quite useless
        // TODO implement them to have same API for everything
    }

}
