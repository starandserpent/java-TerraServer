package com.ritualsoftheold.terra.test;

import com.ritualsoftheold.terra.core.material.MaterialRegistry;
import com.ritualsoftheold.terra.core.material.TerraMaterial;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.data.CriticalBlockBuffer;
import com.ritualsoftheold.terra.offheap.data.WorldDataFormat;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;

import java.util.ArrayList;

public class DummyPalette16ChunkBuffer  implements CriticalBlockBuffer {

    private final MaterialRegistry registry;
    private ArrayList<Integer> IDs;
    private int index;

    public DummyPalette16ChunkBuffer(MaterialRegistry registry) {
        this.registry = registry;
        IDs = new ArrayList<>();
    }

    @Override
    public void close() {
        // Not needed
    }

    @Override
    public void seek(int index) {
        this.index = index;
    }

    @Override
    public int position() {
        return index;
    }

    @Override
    public void next() {
        if(hasNext()) {
            index++;
            //writeWorldId(area, index, 1);
        }
    }

    @Override
    public boolean hasNext() {
        return index < DataConstants.CHUNK_MAX_BLOCKS - 1;
    }

    @Override
    public void write(TerraMaterial material) {
        IDs.add(0);
        IDs.set(index, material.getWorldId());
    }

    @Override
    public TerraMaterial read() {
        int worldId = IDs.get(index);
        return registry.getForWorldId(worldId);
    }


    @Override
    public Object readRef() {
        // TODO critical buffer ref support
        throw new UnsupportedOperationException("TODO implement this");
    }

    @Override
    public void writeRef(Object ref) {
        // TODO critical buffer ref support
        throw new UnsupportedOperationException("TODO implement this");
    }

    @Override
    public WorldDataFormat getDataFormat() {
        return null;
    }

    @Override
    public OffheapChunk.Storage getStorage() {
        return null; // TODO
    }

}