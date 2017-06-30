package com.ritualsoftheold.terra.test;

import java.nio.file.Paths;

import com.jme3.app.SimpleApplication;
import com.ritualsoftheold.terra.files.FileChunkLoader;
import com.ritualsoftheold.terra.files.FileOctreeLoader;
import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;
import com.ritualsoftheold.terra.world.gen.EmptyWorldGenerator;

public class TestGameApp extends SimpleApplication {
    
    private OffheapWorld world;
    
    public static void main(String... args) {
        new TestGameApp().start();
    }
    
    @Override
    public void simpleInitApp() {
        
        world = new OffheapWorld(new FileChunkLoader(Paths.get("chunks")), new FileOctreeLoader(Paths.get("octrees"), 8192),
                new MaterialRegistry(), new EmptyWorldGenerator());
        
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
    }

}
