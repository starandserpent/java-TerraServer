package com.ritualsoftheold.terra.world.gen;

@FunctionalInterface
public interface TriConsumer<T, U, V> {
    
    void accept(T t, U u, V v);
}
