package com.ritualsoftheold.terra.offheap.node;

import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.material.TerraMaterial;
import com.ritualsoftheold.terra.node.Chunk;
import com.ritualsoftheold.terra.node.Node;
import com.ritualsoftheold.terra.node.Octree;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.data.OffheapNode;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

public class OffheapOctree implements Octree, OffheapNode {
    
    private static final Memory mem = OS.memory();
    
    /**
     * Memory address of the octree.
     */
    private long addr;

    private int octreeId;
    
    private MaterialRegistry materialRegistry;
    
    public OffheapOctree(long addr, int octreeId, MaterialRegistry materialRegistry) {
        this.addr = addr;
        this.octreeId = octreeId;
        this.materialRegistry = materialRegistry;
    }
    
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
    public void close() throws Exception {
        // Nothing to be done here...
    }

}
