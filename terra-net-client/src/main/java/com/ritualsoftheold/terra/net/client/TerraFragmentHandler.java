package com.ritualsoftheold.terra.net.client;

import java.lang.invoke.VarHandle;

import com.ritualsoftheold.terra.manager.DataConstants;
import com.ritualsoftheold.terra.net.TerraProtocol;
import org.agrona.DirectBuffer;

import com.ritualsoftheold.terra.memory.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.memory.chunk.ChunkStorage;
import com.ritualsoftheold.terra.memory.chunk.compress.ChunkFormat;
import com.ritualsoftheold.terra.memory.node.OffheapChunk;
import com.ritualsoftheold.terra.manager.octree.OctreeStorage;
import com.ritualsoftheold.terra.manager.verifier.TerraVerifier;

import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

public class TerraFragmentHandler implements FragmentHandler {

    private static final Memory mem = OS.memory();

    private final OctreeStorage octreeStorage;
    
    private final ChunkStorage chunkStorage;
    
    private final TerraVerifier verifier;
    
    public TerraFragmentHandler(OctreeStorage octreeStorage, ChunkStorage chunkStorage, TerraVerifier verifier) {
        this.octreeStorage = octreeStorage;
        this.chunkStorage = chunkStorage;
        this.verifier = verifier;
    }
    
    @Override
    public void onFragment(DirectBuffer buffer, int offset, int length,
            Header header) {
        byte msgType = buffer.getByte(offset);
        offset++;
        
        switch (msgType) {
        case TerraProtocol.MESSAGE_TYPE_OCTREE:
            byte count = buffer.getByte(offset);
            offset++;
            System.out.println("Octree received: " + count);
            for (int i = 0; i < count; i++) {
                // Metadata about the octree
                int id = buffer.getInt(offset);
                verifier.verifyOctreeId(id); // Id must be safe
                boolean subChunks = buffer.getByte(offset + 4) != 0;
                
                // Actual octree data
                long addr = buffer.addressOffset() + offset + 5;
                verifier.verifyOctree(addr, subChunks); // That must be safe too
                offset += DataConstants.OCTREE_SIZE;
                
                // Data is safe, add it to storage
                long octreeAddr = octreeStorage.getOctreeAddrInternal(id); // Acquire pointer to group
                mem.copyMemory(addr, octreeAddr, 33); // Copy this octree data there
                
                VarHandle.fullFence(); // All loads after this see changes
                octreeStorage.setAvailability(id, (byte) 1); // So data is available
            }
            break;
        case TerraProtocol.MESSAGE_TYPE_CHUNK:
            System.out.println("receiving chunk...");
            verifier.verifyChunkLength(length);
            
            byte type = buffer.getByte(offset);
            int chunkId = buffer.getInt(offset + 1);
            verifier.verifyChunkId(chunkId); // Make sure id is safe
            
            ChunkBuffer buf = chunkStorage.getOrLoadBuffer(chunkId >>> 16); // Get the buffer
            
            // Fill in the chunk in the buffer
            OffheapChunk chunk = buf.getChunk(chunkId & 0xffff);
            long addr = mem.allocate(length);
            mem.copyMemory(buffer.addressOffset() + offset + 5, addr, length);
            VarHandle.fullFence(); // Ensure that loads are not reordered in middle of copyMemory
            // (after chunk is available, that is)
            
            OffheapChunk.Storage storage = new OffheapChunk.Storage(ChunkFormat.forType(type), addr, length);
            chunk.setStorageInternal(storage); // This makes chunk available
            break;
        }
    }

}
