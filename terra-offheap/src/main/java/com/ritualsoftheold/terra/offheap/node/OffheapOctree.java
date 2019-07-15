package com.ritualsoftheold.terra.offheap.node;

import com.ritualsoftheold.terra.core.buffer.BlockBuffer;
import com.ritualsoftheold.terra.core.material.MaterialRegistry;
import com.ritualsoftheold.terra.core.material.TerraMaterial;
import com.ritualsoftheold.terra.core.node.Chunk;
import com.ritualsoftheold.terra.core.node.Node;
import com.ritualsoftheold.terra.core.node.Octree;
import com.ritualsoftheold.terra.core.node.OctreeNode;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.ChunkLArray;
import com.ritualsoftheold.terra.offheap.data.OffheapNode;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import net.openhft.chronicle.values.Values;

import java.util.HashMap;

// TODO implement this class or figure out something better
public class OffheapOctree implements Octree, OffheapNode {
    
    private static final Memory mem = OS.memory();
    
    /**
     * Memory address of the octree.
     */
    private long addr;

    private int octreeId;
    
    private MaterialRegistry materialRegistry;

    //This will be out octree, the locational code of the octree node will be used as the key
    private HashMap<Integer,OctreeNode> linearOctree;

    private int x,y,z;

    public OffheapOctree(long addr, int octreeId, MaterialRegistry materialRegistry) {
        this.addr = addr;
        this.octreeId = octreeId;
        this.materialRegistry = materialRegistry;
    }
    public OffheapOctree(MaterialRegistry materialRegistry){
        this.materialRegistry = materialRegistry;
//        IntValue avgKeyTmp = Values.newHeapInstance(IntValue.class);
//        avgKeyTmp.setValue(Integer.MAX_VALUE);
        linearOctree = new HashMap<>();
    }
    //--------------------------------------
    // NEW LINEAR OCTREE METHODS
    //--------------------------------------
    public void SetOctreeOrigin(int x,int y, int z){
        this.x = x;
        this.y = y;
        this.z = z;
    }
    public OctreeNode GetParentNode(OctreeNode node){
        int locParent = node.locCode >> 3;
        return linearOctree.get(locParent);
    }
    public boolean InsertChunkOctree(int chunkLoc, int dx, int dy, int dz){
        //Case 1: Empty Octree
        if(linearOctree.size() == 0){
            OctreeNode octreeNode = new OctreeNode();
            octreeNode.chunkLoc = chunkLoc;
            linearOctree.put(octreeNode.locCode,octreeNode);
            return true;
        }
        return false;
    }
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
