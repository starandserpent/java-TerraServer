package com.ritualsoftheold.terra.test;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.system.AppSettings;
import com.ritualsoftheold.terra.TerraModule;
import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.material.TerraTexture;
import com.ritualsoftheold.terra.mesher.MeshContainer;
import com.ritualsoftheold.terra.mesher.NaiveMesher;
import com.ritualsoftheold.terra.mesher.VoxelMesher;
import com.ritualsoftheold.terra.mesher.resource.TextureManager;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyChunkLoader;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyOctreeLoader;
import com.ritualsoftheold.terra.offheap.memory.MemoryPanicHandler;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;
import com.ritualsoftheold.terra.offheap.world.WorldLoadListener;
import com.ritualsoftheold.terra.world.LoadMarker;
import com.ritualsoftheold.terra.world.gen.WorldGenerator;

import io.netty.buffer.ByteBufAllocator;

public class TestGameApp extends SimpleApplication implements ActionListener {
    
    private OffheapWorld world;
    private LoadMarker player;
    
    private BlockingQueue<Geometry> geomCreateQueue = new ArrayBlockingQueue<>(10000);
    
    private float loadMarkersUpdated;
    
    public static void main(String... args) {
        TestGameApp app = new TestGameApp();
        app.showSettings = false;
        app.settings = new AppSettings(true);
        app.settings.setResolution(1920, 1080);
        app.settings.setTitle("Terra testgame");
        app.settings.setFullscreen(true);
        app.start();
    }
    
    @Override
    public void simpleInitApp() {
        //setDisplayFps(false);
        //setDisplayStatView(false);
        
        
        TerraModule mod = new TerraModule("testgame");
        mod.newMaterial().name("dirt").texture(new TerraTexture(256, 256, "NorthenForestDirt256px.png"));
        mod.newMaterial().name("grass").texture(new TerraTexture(256, 256, "NorthenForestGrass256px.png"));
        MaterialRegistry reg = new MaterialRegistry();
        mod.registerMaterials(reg);
        
        WorldGenerator gen = new TestWorldGenerator();
        gen.initialize(0, reg);
        
        ChunkBuffer.Builder bufferBuilder = new ChunkBuffer.Builder()
                .maxChunks(128)
                .globalQueue(8)
                .chunkQueue(4);
        
        world = new OffheapWorld.Builder()
                .chunkLoader(new DummyChunkLoader())
                .octreeLoader(new DummyOctreeLoader(32768))
                .storageExecutor(ForkJoinPool.commonPool())
                .chunkStorage(bufferBuilder, 128)
                .octreeStorage(32768)
                .generator(gen)
                .generatorExecutor(ForkJoinPool.commonPool())
                .materialRegistry(reg)
                .memorySettings(10000000, 10000000, new MemoryPanicHandler() {
                    
                    @Override
                    public PanicResult outOfMemory(long max, long used, long possible) {
                        return PanicResult.CONTINUE;
                    }
                    
                    @Override
                    public PanicResult goalNotMet(long goal, long possible) {
                        return PanicResult.CONTINUE;
                    }
                })
                .build();
                
        player = new LoadMarker(0, 0, 0, 64, 64, 0);
        world.addLoadMarker(player);
        
        TextureManager texManager = new TextureManager(assetManager); // Initialize texture atlas/array manager
        texManager.loadMaterials(reg); // And make it load material registry
        
        // Create material
        Material mat = new Material(assetManager, "terra/shader/TerraArray.j3md");
        //mat.getAdditionalRenderState().setWireframe(true);
        //mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        mat.setTexture("ColorMap", texManager.getGroundTexture());
        
        VoxelMesher mesher = new NaiveMesher();
        
        world.setLoadListener(new WorldLoadListener() {

            @Override
            public void octreeLoaded(long addr, long groupAddr, int id, float x,
                    float y, float z, float scale, LoadMarker trigger) {
                // For now, just ignore octrees
            }

            @Override
            public void chunkLoaded(OffheapChunk chunk, float x, float y, float z, LoadMarker trigger) {
                Vector3f center = cam.getLocation();
                if (Math.abs(x - center.x) > 128
                        || Math.abs(y - center.y) > 128
                        || Math.abs(z - center.z) > 128) {
                    //System.out.println("too far away: " + x + ", " + y + ", " + z);
                    return;
                }
                
                //System.out.println("Loaded chunk: " + chunk.memoryAddress());
                MeshContainer container = new MeshContainer(200, ByteBufAllocator.DEFAULT);
                mesher.chunk(chunk.newIterator(), texManager, container);
                
                // Create mesh
                Mesh mesh = new Mesh();
                //System.out.println(mesher.getVertices());
                //System.out.println(mesher.getIndices());
                mesh.setBuffer(Type.Position, 1, container.getVertices().nioBuffer().asFloatBuffer());
                mesh.setBuffer(Type.Index, 3, container.getIndices().nioBuffer().asIntBuffer());
                mesh.setBuffer(Type.TexCoord, 2, container.getTextureCoordinates().nioBuffer().asFloatBuffer());
                
                // Create geometry
                Geometry geom = new Geometry("chunk:" + x + "," + y + "," + z, mesh);
                //mat.setParam("SeparateTexCoord", VarType.Boolean, true);
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
        List<CompletableFuture<Void>> markers = world.updateLoadMarkers();
        markers.forEach((f) -> {
            f.join();
        });
        
        inputManager.addMapping("RELOAD", new KeyTrigger(KeyInput.KEY_G));
        inputManager.addListener(this, "RELOAD");
        
        rootNode.addLight(new AmbientLight());
    }
    
    @Override
    public void simpleUpdate(float tpf) {
        loadMarkersUpdated += tpf;
        if (loadMarkersUpdated > 1) {
            loadMarkersUpdated = 0;
            Vector3f camLoc = cam.getLocation();
            //System.out.println(camLoc);
            player.move(camLoc.getX(), camLoc.getY(), camLoc.getZ());
            //CompletableFuture.runAsync(() -> {
                //long stamp = world.enter();
                //world.updateLoadMarkers(); // Update load markers
                //world.leave(stamp);
            //});
        }
            
        while (!geomCreateQueue.isEmpty()) {
            Geometry geom = geomCreateQueue.poll();
            //System.out.println("create geom: " + geom.getLocalTranslation());
            rootNode.attachChild(geom);
        }
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name == "RELOAD" && isPressed) {
            rootNode.detachAllChildren();
            world.updateLoadMarkers();
        }
    }

}
