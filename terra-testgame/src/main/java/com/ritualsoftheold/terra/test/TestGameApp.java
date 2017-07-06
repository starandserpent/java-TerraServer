package com.ritualsoftheold.terra.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.jme3.app.SimpleApplication;
import com.ritualsoftheold.terra.TerraModule;
import com.ritualsoftheold.terra.files.FileChunkLoader;
import com.ritualsoftheold.terra.files.FileOctreeLoader;
import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.material.TerraTexture;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;
import com.ritualsoftheold.terra.offheap.world.WorldLoadListener;
import com.ritualsoftheold.terra.world.LoadMarker;

import net.openhft.chronicle.core.io.IORuntimeException;

public class TestGameApp extends SimpleApplication {
    
    private OffheapWorld world;
    private LoadMarker player;
    
    public static void main(String... args) {
        new TestGameApp().start();
    }
    
    @Override
    public void simpleInitApp() {
        TerraModule mod = new TerraModule("testgame");
        mod.newMaterial().name("dirt").texture(new TerraTexture(256, 256, "NorthenForestDirt256px.png"));
        MaterialRegistry reg = new MaterialRegistry();
        mod.registerMaterials(reg);
        
        try {
            world = new OffheapWorld(new FileChunkLoader(Files.createDirectories(Paths.get("chunks"))), new FileOctreeLoader(Files.createDirectories(Paths.get("octrees")), 8192),
                    reg, new TestWorldGenerator());
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        player = new LoadMarker(0, 0, 0, 100, 200);
        world.addLoadMarker(player);
        
        world.setLoadListener(new WorldLoadListener() {
            
            @Override
            public void octreeLoaded(long addr, float x, float y, float z, float scale) {
                // For now, just ignore octrees
            }
            
            @Override
            public void chunkLoaded(long addr, float x, float y, float z) {
                System.out.println("Loaded chunk!");
            }
        });
        
        // TODO: Test plan...
        /* 1. Finish changes to support meshing in OffheapWorld (loadArea/loadAll "mesher callbacks")
         * 2. Write code for those callbacks that just calls mesher (this is simple test, remember)
         * 3. Create objects for meshes (copy-paste code from mesher tests, maybe)
         * 4. Does it crash? Is the world just empty, or is there garbage (is memory zeroed correctly)?
         * 5. Implement flatworld terrain generator that actually generates something.
         * 6. Now, does it crash? Is something displayed? Is what is displayed somewhat what is should be?
         * 7. Performance check: do we need to optimize NOW or can we proceed to other tasks
         * 8. Implement non-flatworld generator; check that culling works correctly
         * 9. Bugs check: give the binary to team and hope they can't get it crashing
         * 10. Fix any last bugs in Terra
         * 
         * Then? Networking.
         */
        world.updateLoadMarkers();
    }
    
    @Override
    public void simpleUpdate(float tpf) {
        //world.updateLoadMarkers(); // Update load markers (TODO do this less often)
    }

}
