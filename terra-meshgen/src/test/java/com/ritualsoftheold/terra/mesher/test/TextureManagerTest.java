package com.ritualsoftheold.terra.mesher.test;

import com.jme3.app.SimpleApplication;
import com.ritualsoftheold.terra.TerraModule;
import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.material.TerraTexture;
import com.ritualsoftheold.terra.mesher.resource.TextureManager;

public class TextureManagerTest extends SimpleApplication {

    public static void main(String... args) {
        new TextureManagerTest().start();
    }
    
    @Override
    public void simpleInitApp() {
        TextureManager manager = new TextureManager(assetManager); // jME provides asset manager
        MaterialRegistry registry = new MaterialRegistry();
        
        TerraModule mod = new TerraModule("test");
        mod.newMaterial().name("grass").texture(new TerraTexture(64, 64, "grass.png"));
        mod.newMaterial().name("dirt").texture(new TerraTexture(64, 64, "dirt.png"));
        mod.registerMaterials(registry);
        
        manager.loadMaterials(registry);
    }
    
    
}
