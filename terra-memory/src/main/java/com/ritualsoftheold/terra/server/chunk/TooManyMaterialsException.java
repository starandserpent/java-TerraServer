package com.ritualsoftheold.terra.server.chunk;

public class TooManyMaterialsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TooManyMaterialsException() {
        super(null, null, false, false);
    }
}
