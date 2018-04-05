package com.ritualsoftheold.terra.buffer;

/**
 * References in Terra allow blocks to store more data than just
 * their materials.
 *
 */
public interface TerraRef {
    
    /**
     * Gets type of the reference.
     * @return Reference type.
     */
    Class<?> getType();
    
    /**
     * Gets the value of reference. If this is a mutable reference, changes
     * to it will be reflected to this reference.
     * @return Value of the reference.
     */
    Object get();
    
    /**
     * Sets value of this reference.
     * @param o New value.
     */
    void set(Object o);
    
    /**
     * Creates an immutable copy of this reference.
     * @return Immutable reference.
     */
    TerraRef immutableRef();
    
    /**
     * Checks if this is a mutable reference.
     * @return If this is mutable reference.
     */
    boolean isMutable();
}
