package com.ritualsoftheold.terra.manager;


import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import com.ritualsoftheold.terra.manager.memory.MemoryAllocator;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;


/**
 * A memory area with optional bounds checks.
 *
 */
public class MemoryArea {
    
    // TODO warn about memory leaks created by forgotten release()
    
    private static final Memory mem = OS.memory();
    
    private static final VarHandle releaseAllowedVar;
    
    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            releaseAllowedVar = lookup.findVarHandle(MemoryArea.class, "releaseAllowed", boolean.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e); // Cannot use MemoryAreas safely without this
        }
    }
    
    /**
     * Creates a new memory area by allocating more memory.
     * @param allocator Memory allocator to use.
     * @param length How much memory is needed.
     * @return A new memory area.
     */
    public static MemoryArea create(MemoryAllocator allocator, long length) {
        return new MemoryArea(allocator.allocate(length), length, allocator, true);
    }
    
    /**
     * Creates a new memory area by wrapping a pointer.
     * Releasing it must be done manually, i.e. calling {@link #release()}
     * will throw an exception.
     * @param start Start address.
     * @param length Wanted length.
     * @return A new memory area wrapping a pointer.
     */
    public static MemoryArea wrap(@Pointer long start, long length) {
        return new MemoryArea(start, length, null, false);
    }
    
    /**
     * Start memory address of this.
     */
    @Pointer
    private final long start;
    
    /**
     * Length of this.
     */
    private final long length;
    
    /**
     * Memory allocator used to allocate this area.
     * May be null if this was just wrapped.
     */
    private final MemoryAllocator allocator;
    
    /**
     * Whether or not releasing this memory are is allowed.
     */
    @SuppressWarnings("unused") // VarHandle
    private volatile boolean releaseAllowed;
    
    protected MemoryArea(long start, long length, MemoryAllocator allocator, boolean releaseAllowed) {
        this.start = start;
        this.length = length;
        this.allocator = allocator;
        this.releaseAllowed = releaseAllowed;
    }
    
    public long length() {
        return length;
    }
    
    public void fill(long index, long size, byte fill) {
        mem.setMemory(BuildConfig.inBounds(start + index, 1, start, length), size, fill);
    }
    
    public byte readByte(long index) {
        return mem.readByte(BuildConfig.inBounds(start + index, 1, start, length));
    }
    
    public void writeByte(long index, byte value) {
        mem.writeByte(BuildConfig.inBounds(start + index, 1, start, length), value);
    }
    
    public byte readVolatileByte(long index) {
        return mem.readVolatileByte(BuildConfig.inBounds(start + index, 1, start, length));
    }
    
    public void writeVolatileByte(long index, byte value) {
        mem.writeVolatileByte(BuildConfig.inBounds(start + index, 1, start, length), value);
    }
    
    public short readShort(long index) {
        return mem.readShort(BuildConfig.inBounds(start + index, 2, start, length));
    }
    
    public void writeShort(long index, short value) {
        mem.writeShort(BuildConfig.inBounds(start + index, 2, start, length), value);
    }
    
    public short readVolatileShort(long index) {
        return mem.readVolatileShort(BuildConfig.inBounds(start + index, 2, start, length));
    }
    
    public void writeVolatileShort(long index, short value) {
        mem.writeVolatileShort(BuildConfig.inBounds(start + index, 2, start, length), value);
    }
    
    public int readInt(long index) {
        return mem.readInt(BuildConfig.inBounds(start + index, 4, start, length));
    }
    
    public void writeInt(long index, int value) {
        mem.writeInt(BuildConfig.inBounds(start + index, 4, start, length), value);
    }
    
    public int readVolatileInt(long index) {
        return mem.readVolatileInt(BuildConfig.inBounds(start + index, 4, start, length));
    }
    
    public void writeVolatileInt(long index, int value) {
        mem.writeVolatileInt(BuildConfig.inBounds(start + index, 4, start, length), value);
    }
    
    public long readLong(long index) {
        return mem.readLong(BuildConfig.inBounds(start + index, 8, start, length));
    }
    
    public void writeLong(long index, long value) {
        mem.writeLong(BuildConfig.inBounds(start + index, 8, start, length), value);
    }
    
    public long readVolatileLong(long index) {
        return mem.readVolatileLong(BuildConfig.inBounds(start + index, 8, start, length));
    }
    
    public void writeVolatileLong(long index, long value) {
        mem.writeVolatileLong(BuildConfig.inBounds(start + index, 8, start, length), value);
    }
    
    public int getAndAddInt(long index, int increment) {
        return mem.addInt(BuildConfig.inBounds(start + index, 4, start, length), increment);
    }
    
    public long getAndAddLong(long index, int increment) {
        return mem.addLong(BuildConfig.inBounds(start + index, 8, start, length), increment);
    }
    
    /**
     * Releases this memory area. Do not call this more than once!
     */
    public void release() {
        // Atomic: only one release() ever gets through
        if (((boolean) releaseAllowedVar.getAndSet(this, false))) {
            throw new IllegalStateException("release not allowed");
        }
        allocator.free(start, length);
    }
    
    /**
     * Gets the memory address of this memory area.
     * @return Memory address.
     */
    @Pointer
    public long memoryAddress() {
        return start;
    }
}
