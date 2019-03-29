package com.ritualsoftheold.terra.gen.interfaces;

@FunctionalInterface
public interface TriConsumer<T, U, V> {
    
    void accept(T t, U u, V v);
}
