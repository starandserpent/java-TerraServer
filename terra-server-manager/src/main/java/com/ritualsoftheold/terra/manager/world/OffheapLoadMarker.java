package com.ritualsoftheold.terra.manager.world;

import com.ritualsoftheold.terra.manager.gen.objects.LoadMarker;
import com.ritualsoftheold.terra.manager.util.IntFlushList;

public class OffheapLoadMarker extends LoadMarker implements Cloneable {

    private final IntFlushList octrees;

    protected OffheapLoadMarker(float x, float y, float z, float hardRadius, float softRadius, int priority) {
        super(x, y, z, hardRadius, softRadius, priority);
        this.octrees = new IntFlushList(64, 2); // TODO tune these settings
    }

    /**
     * Clone constructor.
     * @param another Another to clone.
     */
    protected OffheapLoadMarker(OffheapLoadMarker another) {
        super(another.getX(), another.getY(), another.getZ(), another.getHardRadius(), another.getSoftRadius(), another.getPriority());
        this.octrees = another.octrees.clone();
    }
    
    public OffheapLoadMarker clone() {
        return new OffheapLoadMarker(this);
    }
}
