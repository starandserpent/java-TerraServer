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


// TODO: Terra learning and documentation project. I want the comments to read out like a good novel. Easy, plain.


// TODO, DOCS: Brief overview of what we're doing here/what is the purpose of TestGameApp.
public class TestGameApp extends SimpleApplication implements ActionListener {

    // TODO, DOCS: Explain OffheapWorld, why OffheapWorld specifically, are there alternatives?
    private OffheapWorld world;
    // TODO, DOCS: Javadocs is lacking. Practical explanation is required. Expanded explanation is also
    // TODO, DOCS: apparently required judging from the use of word "some" -> Go over use cases.
    private LoadMarker player;

    // TODO, DOCS: What is this used for? Explanation needed.
    private BlockingQueue<Geometry> geomCreateQueue = new ArrayBlockingQueue<>(10000);

    // TODO, DOCS: What is this used for? Explanation needed.
    private float loadMarkersUpdated;
    
    public static void main(String... args) {

        // Create an instance for our test game and configure it.
        TestGameApp app = new TestGameApp();                // Create an instance for our test game.
        app.showSettings = false;                           // TODO, DOCS: What exactly does this do?
        app.settings = new AppSettings(true);    // TODO, DOCS: What exactly does this do? Defaults? Where?
        app.settings.setResolution(1024, 768);  // Set resolution for the test game.
        app.settings.setTitle("Terra testgame");            // Set app title for the operating system.
        app.settings.setFullscreen(false);                  // Fullscreen (true) / window mode (false).
        app.start();                                        // Start the app.
    }
    
    @Override
    public void simpleInitApp() { // TODO, DOCS: Brief overview of simpleInitApp.
        //setDisplayFps(false);
        //setDisplayStatView(false);

        TerraModule mod = new TerraModule("testgame"); // TODO, DOCS: What IS a TerraModule. What uses it? How?
        // TODO, DOCS: Explain clearly what materials ARE, what they REQUIRE, what is supported, where to store resources.
        mod.newMaterial().name("dirt").texture(new TerraTexture(256, 256, "NorthenForestDirt256px.png"));
        mod.newMaterial().name("grass").texture(new TerraTexture(256, 256, "NorthenForestGrass256px.png"));
        MaterialRegistry reg = new MaterialRegistry(); // TODO, DOCS: Explain MaterialRegistry.
        mod.registerMaterials(reg); // Register the materials we defined.
        
        WorldGenerator<?> gen = new TestWorldGenerator(); // Create an instance of the world generator we want to use.
        gen.setup(0, reg); // And set it up to use our materials.

        // TODO, DOCS: Explain what is happening here.
        ChunkBuffer.Builder bufferBuilder = new ChunkBuffer.Builder()
                .maxChunks(128)
                .queueSize(4);

        // Builder for our world.
        world = new OffheapWorld.Builder()
                .chunkLoader(new DummyChunkLoader()) // TODO, DOCS: ?
                .octreeLoader(new DummyOctreeLoader(32768)) // TODO, DOCS: ?
                .storageExecutor(ForkJoinPool.commonPool()) // TODO, DOCS: ?
                .chunkStorage(bufferBuilder, 128) // TODO, DOCS: ?
                .octreeStorage(32768) // TODO, DOCS: ?
                .generator(gen) // Use our world generator.
                .generatorExecutor(ForkJoinPool.commonPool()) // TODO, DOCS: ?
                .materialRegistry(reg) // Use our materials.
                .memorySettings(10000000, 10000000, new MemoryPanicHandler() { // TODO, DOCS: ?

                    // TODO, DOCS: Why is this handled in the implementation? Explain.
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

        // TODO, DOCS: Needs a little bit more hand holding. What, Why.
        player = world.createLoadMarker(0, 0, 0, 64, 64, 0);
        world.addLoadMarker(player);

        // TODO, DOCS: I can see you're doing that. You don't give my what/why. You should.
        TextureManager texManager = new TextureManager(assetManager); // Initialize texture atlas/array manager
        // TODO, DOCS: It seems we're putting "reg" here and there. Might need some clarification as to why.
        texManager.loadMaterials(reg); // And make it load material registry

        // TODO, DOCS: What is going on here -> more information: What, Why.
        // Create material
        Material mat = new Material(assetManager, "terra/shader/TerraArray.j3md");  // TODO, DOCS: ?
        //mat.getAdditionalRenderState().setWireframe(true);
        //mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        mat.setTexture("ColorMap", texManager.getGroundTexture());  // TODO, DOCS: ?

        /*
        Initialize mesher. Creates meshes out of voxel data. We're using naive mesher here. A naive mesher does not
        optimize the outputted mesh in any way. This is not an ideal solution in any circumstance beyond testing.
        */
        VoxelMesher mesher = new NaiveMesher();

        // TODO, DOCS: What is this?
        world.setLoadListener(new WorldLoadListener() {

            // TODO, DOCS: Javadocs -> Saved TO storage? Loaded FROM storage? What storage?
            @Override
            public void octreeLoaded(long addr, long groupAddr, int id, float x,
                    float y, float z, float scale, LoadMarker trigger) {
                // For now, just ignore octrees
                // TODO, DOCS: Why?
            }
            // TODO, DOCS: Again, needs quite a bit more explanations.
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
                // TODO, DOCS: Javadocs -> In what way? Gotta do better than this.
                MeshContainer container = new MeshContainer(200, ByteBufAllocator.DEFAULT);
                mesher.chunk(chunk.getBuffer(), texManager, container);  // TODO, DOCS: ?
                
                // Create mesh
                Mesh mesh = new Mesh();  // TODO, DOCS: Really? Wouldn't have guessed. For what, why, where?
                //System.out.println(mesher.getVertices());
                //System.out.println(mesher.getIndices());
                // TODO, DOCS: ?
                mesh.setBuffer(Type.Position, 1, container.getVertices().nioBuffer().asFloatBuffer());
                mesh.setBuffer(Type.Index, 3, container.getIndices().nioBuffer().asIntBuffer());
                mesh.setBuffer(Type.TexCoord, 2, container.getTextureCoordinates().nioBuffer().asFloatBuffer());
                
                // Create geometry
                // TODO, DOCS: What, why, where?
                Geometry geom = new Geometry("chunk:" + x + "," + y + "," + z, mesh);
                //mat.setParam("SeparateTexCoord", VarType.Boolean, true);
                geom.setMaterial(mat);
                //geom.setLocalScale(0.5f);
                geom.setLocalTranslation(x, y, z); // TODO, DOCS: ?
                geom.setCullHint(CullHint.Never); // TODO, DOCS: ?
                
                container.release(); // TODO, DOCS: Did we do some Netty stuff I missed? Redundant?
                
                // Place geometry in queue for main thread
                geomCreateQueue.add(geom); // TODO, DOCS: Because... ? Otherwise it's just another pointless comment.
            }
        });
        
        // Some config options
        flyCam.setMoveSpeed(10); // TODO, DOCS: World units per second is a bit vague. Don't want to read through
                                 // TODO, DOCS: tons of docs / recompile to figure out a simple thing. Conversions?
        rootNode.setCullHint(CullHint.Never); // TODO, DOCS: Needs a bit more handholding.
        
        List<CompletableFuture<Void>> markers = world.updateLoadMarkers(); // TODO, DOCS: ?
        markers.forEach((f) -> {
            f.join();
        });

        // TODO, DOCS: So... reload by pressing  "G"? Intuitive, because... ? Needed for... ? Needs more info.
        inputManager.addMapping("RELOAD", new KeyTrigger(KeyInput.KEY_G));
        inputManager.addListener(this, "RELOAD");
        
        rootNode.addLight(new AmbientLight()); // Add a simple global ambient light in the scene so we can actually see.
    }

    // TODO, DOCS: Proper explanations for each line.
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

    // TODO, DOCS: ?
    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name == "RELOAD" && isPressed) {
            rootNode.detachAllChildren(); // TODO, DOCS: Javadocs -> Which means what exactly, in practice?
            world.updateLoadMarkers(); // TODO, DOCS: ?
        }
    }

}
