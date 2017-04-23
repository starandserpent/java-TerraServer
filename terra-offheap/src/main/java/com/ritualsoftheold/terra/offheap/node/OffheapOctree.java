package com.ritualsoftheold.terra.offheap.node;

import com.ritualsoftheold.terra.node.Block;
import com.ritualsoftheold.terra.node.Chunk;
import com.ritualsoftheold.terra.node.Node;
import com.ritualsoftheold.terra.node.Octree;
import com.ritualsoftheold.terra.node.SimpleBlock;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.data.OffheapNode;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;

import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

// TODO need some API changes for callback stuff
public class OffheapOctree implements Octree, OffheapNode {
    
    private static Memory mem = OS.memory();
    
    /**
     * Memory address of the octree.
     */
    private long address;
    
    /**
     * Is this node valid.
     */
    private boolean valid = false;
    
    /**
     * Scale of this octree. Required to see if children might be chunks.
     */
    private float scale;
    
    /**
     * Reference to world of this octree.
     */
    private OffheapWorld world;
    
    public OffheapOctree(OffheapWorld world, float scale) {
        this.world = world;
        this.scale = scale;
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
    public Block getBlockAt(int index) {
        // Read first 2 bytes from node; they serve as block id
        // TODO block size with octree
        return new SimpleBlock(world.getMaterialRegistry().getForWorldId(mem.readShort(address +  index * DataConstants.OCTREE_NODE_SIZE)), 0);
    }

    @Override
    public Chunk getChunkAt(int index) {
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

    @Override
    public int l_getDataSize() {
        return DataConstants.OCTREE_SIZE;
    }

    @Override
    public long l_getNodeAddr(int index) {
        // Address + metadata + size of node * index
        return address + 1 + DataConstants.OCTREE_NODE_SIZE * index;
    }

    @Override
    public int l_getNodeAt(int index) {
        return mem.readInt(l_getNodeAddr(index));
    }

    @Override
    public void l_getData(int[] data) {
        if (data.length < 9)
            throw new IllegalArgumentException("data array must be at least 9 ints");
        // First int, least significant byte: flags
        // Other ints: octree data
        mem.copyMemory(address, data, DataConstants.ARRAY_DATA + 2, DataConstants.OCTREE_SIZE);
    }

    @Override
    public long memoryAddress() {
        return address;
    }

    @Override
    public void memoryAddress(long addr) {
        valid = true;
        address = addr;
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public void invalidate() {
        valid = false;
    }

}
