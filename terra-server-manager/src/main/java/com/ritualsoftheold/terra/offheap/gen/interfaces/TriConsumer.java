package com.ritualsoftheold.terra.offheap.gen.interfaces;

@FunctionalInterface
public interface TriConsumer<T, U, V> {
    
    void accept(T t, U u, V v);
}
