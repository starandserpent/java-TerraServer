package com.ritualsoftheold.terra.offheap.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * A concurrent array-backed list. When enlarging the array, it briefly blocks;
 * otherwise, it is both non-blocking and lock-free.
 *
 */
public class IntFlushList implements Cloneable {
    
    private static final VarHandle arrayVar = MethodHandles.arrayElementVarHandle(int[].class);
    private static final VarHandle arrayFieldVar;
    private static final VarHandle slotVar;
    
    static {
        try {
            arrayFieldVar = MethodHandles.lookup().findVarHandle(IntFlushList.class, "array", int[].class);
            slotVar = MethodHandles.lookup().findVarHandle(IntFlushList.class, "slot", int.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }
    }
    
    @SuppressWarnings("unused") // VarHandle
    private int[] array;
    
    @SuppressWarnings("unused") // VarHandle
    private int slot;
    
    private final int initialSize;
    private final float sizeMultiplier;
    
    public IntFlushList(int initialSize, float sizeMultiplier) {
        this.initialSize = initialSize;
        arrayFieldVar.setRelease(this, new int[initialSize]);
        slotVar.setRelease(this, 0);
        this.sizeMultiplier = sizeMultiplier;
    }
    
    public IntFlushList(IntFlushList origin) {
        this.initialSize = origin.initialSize;
        arrayFieldVar.setRelease(this, arrayFieldVar.getAcquire(origin));
        slotVar.setRelease(this, slotVar.getAcquire(origin));
        this.sizeMultiplier = origin.sizeMultiplier;
    }
    
    /**
     * Adds a value to the list.
     * @param value Value.
     */
    public void add(int value) {
        int place;
        int[] arrRef;
        for (place = (int) slotVar.getAndAdd(this, 1);;) { // Need a bigger array
            arrRef = (int[]) arrayFieldVar.getAcquire(this);
            int length = arrRef.length;
            if (length > place) {
                break; // Found a place!
            }
            if (reallocArray(length)) {
                arrRef = (int[]) arrayFieldVar.getAcquire(this);
            }
        }
        
        // Write until we write to current array
        for (arrayVar.setRelease(arrRef, place, value);;) {
            int[] newRef = (int[]) arrayFieldVar.getAcquire(this);
            if (arrRef == newRef) {
                break; // Write ended up in correct array
            }
            arrRef = newRef;
        }
    }
    
    private synchronized boolean reallocArray(int length) {
        int[] arrRef = (int[]) arrayFieldVar.getAcquire(this);
        if (arrRef.length > length) {
            return false; // Someone got there before us!
        }
        
        int newSize = (int) (arrRef.length * sizeMultiplier);
        int[] newArray = new int[newSize];
        System.arraycopy(arrRef, 0, newArray, 0, length);
        VarHandle.fullFence(); // Hopefully enough to prevent arraycopy visibility issues
        arrayFieldVar.setRelease(this, newArray);
        return true;
    }
    
    /**
     * Gets backing array of this list. Note that if more entries are added,
     * it might change in future. Also note that plain reads may cause memory
     * visibility issues unless you really know what you are doing.
     * @return Contents of the list.
     */
    public int[] getArray() {
        return (int[]) arrayFieldVar.getAcquire(this);
    }

    /**
     * Clears the backing array of this list. If calls to {@link #add(int)}
     * are in progress, this does not work reliably.
     */
    public void clear() {
        arrayFieldVar.setRelease(this, new int[initialSize]);
    }
    
    @Override
    public IntFlushList clone() {
        return new IntFlushList(this);
    }
}
