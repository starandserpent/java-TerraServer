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
     * Creates a copy of this reference that is mutable. Changes to it
     * will also be reflected in this reference.
     * @return Mutable reference.
     */
    TerraRef mutableRef();
    
    /**
     * Checks if this is a mutable reference.
     * @return If this is mutable reference.
     */
    boolean isMutable();
}
