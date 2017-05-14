package com.ritualsoftheold.terra.mesher.test;

import org.junit.Before;

import com.jme3.app.SimpleApplication;
import com.ritualsoftheold.terra.mesher.resource.TextureManager;

public class TextureManagerTest extends SimpleApplication {

    public static void main(String... args) {
        new TextureManagerTest().start();
    }
    
    @Override
    public void simpleInitApp() {
        TextureManager manager = new TextureManager(assetManager); // jME provides asset manager
        
        // TODO blocked on material registry changes
    }
    
    
}
