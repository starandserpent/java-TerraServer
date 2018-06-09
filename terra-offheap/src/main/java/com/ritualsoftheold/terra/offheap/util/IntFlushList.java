package com.ritualsoftheold.terra.offheap.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * A concurrent array-backed list. When enlarging the array, it briefly blocks;
 * otherwise, it is both non-blocking and lock-free.
 *
 */
public class IntFlushList {
    
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
    
    private final float sizeMultiplier;
    
    public IntFlushList(int initialSize, float sizeMultiplier) {
        arrayFieldVar.setRelease(this, new int[initialSize]);
        slotVar.setRelease(this, 0);
        this.sizeMultiplier = sizeMultiplier;
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
        arrayFieldVar.setRelease(this, newArray);
        return true;
    }
    
    /**
     * Gets the array. Note that it is not safe to use this operation if a call
     * to {@link #add(int)} is in progress.
     * @return Contents of the list.
     */
    public int[] getArray() {
        return (int[]) arrayFieldVar.getAcquire(this);
    }
}
