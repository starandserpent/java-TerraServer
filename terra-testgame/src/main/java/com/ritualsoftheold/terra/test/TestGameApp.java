package com.ritualsoftheold.terra.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.shader.VarType;
import com.jme3.util.BufferUtils;
import com.ritualsoftheold.terra.TerraModule;
import com.ritualsoftheold.terra.files.FileChunkLoader;
import com.ritualsoftheold.terra.files.FileOctreeLoader;
import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.material.TerraTexture;
import com.ritualsoftheold.terra.mesher.NaiveMesher;
import com.ritualsoftheold.terra.mesher.VoxelMesher;
import com.ritualsoftheold.terra.mesher.resource.TextureManager;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyChunkLoader;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;
import com.ritualsoftheold.terra.offheap.world.WorldLoadListener;
import com.ritualsoftheold.terra.world.LoadMarker;

import net.openhft.chronicle.core.io.IORuntimeException;

public class TestGameApp extends SimpleApplication {
    
    private OffheapWorld world;
    private LoadMarker player;
    
    private BlockingQueue<Geometry> geomCreateQueue = new ArrayBlockingQueue<>(10000);
    
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
            world = new OffheapWorld(new DummyChunkLoader(), new FileOctreeLoader(Files.createDirectories(Paths.get("octrees")), 8192),
                    reg, new TestWorldGenerator());
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        player = new LoadMarker(0, 0, 0, 32, 200);
        world.addLoadMarker(player);
        
        TextureManager texManager = new TextureManager(assetManager); // Initialize texture atlas/array manager
        texManager.loadMaterials(reg); // And make it load material registry
        
        world.setLoadListener(new WorldLoadListener() {
            
            @Override
            public void octreeLoaded(long addr, float x, float y, float z, float scale) {
                // For now, just ignore octrees
            }
            
            @Override
            public void chunkLoaded(long addr, float x, float y, float z) {
                System.out.println("Loaded chunk: " + addr);
                VoxelMesher mesher = new NaiveMesher(); // Not thread safe, but this is still performance hog!
                mesher.chunk(addr, texManager);
                
                // Create mesh
                Mesh mesh = new Mesh();
                //System.out.println(mesher.getVertices());
                //System.out.println(mesher.getIndices());
                mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(mesher.getVertices().toFloatArray()));
                mesh.setBuffer(Type.Index, 3, BufferUtils.createIntBuffer(mesher.getIndices().toIntArray()));
                mesh.setBuffer(Type.TexCoord, 3, BufferUtils.createFloatBuffer(mesher.getTextureCoords().toFloatArray()));
                
                // Create geometry
                Geometry geom = new Geometry("chunk:" + x + "," + y + "," + z, mesh);
                Material mat = new Material(assetManager, "jme3test/texture/UnshadedArray.j3md");
                mat.getAdditionalRenderState().setWireframe(true);
                //mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
                mat.setTexture("ColorMap", texManager.getGroundTexture());
                mat.setParam("SeparateTexCoord", VarType.Boolean, true);
                geom.setMaterial(mat);
                //geom.setLocalScale(0.5f);
                geom.setLocalTranslation(x, y, z);
                geom.setCullHint(CullHint.Never);
                
                // Place geometry in queue for main thread
                geomCreateQueue.add(geom);
            }
        });
        
        // Some config options
        flyCam.setMoveSpeed(10);
        rootNode.setCullHint(CullHint.Never);
        
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
        //world.updateLoadMarkers(); // Update load markers (TODO schedule this)
        while (!geomCreateQueue.isEmpty()) {
            Geometry geom = geomCreateQueue.poll();
            System.out.println("create geom: " + geom.getLocalTranslation());
            rootNode.attachChild(geom);
        }
    }

}
