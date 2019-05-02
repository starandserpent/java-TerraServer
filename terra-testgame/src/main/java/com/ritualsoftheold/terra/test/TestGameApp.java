package com.ritualsoftheold.terra.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.jme3.texture.TextureArray;
import com.jme3.util.BufferUtils;
import com.ritualsoftheold.terra.core.TerraModule;
import com.ritualsoftheold.terra.mesher.GreedyMesher;
import com.ritualsoftheold.terra.offheap.chunk.ChunkBuffer;
import com.ritualsoftheold.terra.core.gen.interfaces.world.WorldGeneratorInterface;
import com.ritualsoftheold.terra.core.gen.objects.LoadMarker;
import com.ritualsoftheold.terra.core.material.MaterialRegistry;
import com.ritualsoftheold.terra.core.material.TerraTexture;
import com.ritualsoftheold.terra.offheap.memory.MemoryPanicHandler;
import com.ritualsoftheold.terra.mesher.resource.TextureManager;
import com.ritualsoftheold.terra.offheap.node.OffheapChunk;
import com.ritualsoftheold.terra.offheap.world.OffheapWorld;
import com.ritualsoftheold.terra.offheap.world.WorldLoadListener;
import com.ritualsoftheold.terra.mesher.MeshContainer;
import com.ritualsoftheold.terra.mesher.VoxelMesher;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyChunkLoader;
import com.ritualsoftheold.terra.offheap.io.dummy.DummyOctreeLoader;
import jme3tools.optimize.GeometryBatchFactory;
import jme3tools.optimize.LodGenerator;

public class TestGameApp extends SimpleApplication implements ActionListener {

    private OffheapWorld world;
    private LoadMarker player;
    private boolean wireframe = false;
    private Material mat;
    private int index;

    private BlockingQueue<Geometry> geomCreateQueue = new ArrayBlockingQueue<>(10000);

    private float loadMarkersUpdated;

    public static void main(String... args) {
        TestGameApp app = new TestGameApp();
        app.showSettings = false;
        app.settings = new AppSettings(true);
        app.settings.setResolution(1600, 900);
        app.settings.setTitle("Terra testgame");
        app.settings.setFullscreen(false);
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

        TextureManager texManager = new TextureManager(assetManager, reg);
        TextureArray atlasTexture = texManager.getTextureArray();
        atlasTexture.setWrap(Texture.WrapMode.Repeat);
        atlasTexture.setMagFilter(Texture.MagFilter.Nearest);
        atlasTexture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);

        mat = new Material(assetManager, "/shaders/terra/TerraArray.j3md");
        mat.setTexture("ColorMap", atlasTexture);

        WorldGeneratorInterface<?> gen = new WorldGenerator();
        gen.setup(0, reg);

        ChunkBuffer.Builder bufferBuilder = new ChunkBuffer.Builder()
                .maxChunks(128)
                .queueSize(4);

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

        player = world.createLoadMarker(0, 0, 0, 1, 1, 0);
        // LoadMarker secondchunk = world.createLoadMarker(56+16+32,0, 56+16+32, 32, 32, 0);

        world.addLoadMarker(player);
        //  world.addLoadMarker(secondchunk);

        VoxelMesher mesher = new GreedyMesher();



        world.setLoadListener(new WorldLoadListener() {

            int lol = 0;
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
                    return;
                }

                lol++;
                System.out.println(lol);

                //System.out.println("Loaded chunk: " + chunk.memoryAddress());
                MeshContainer container = new MeshContainer();
                mesher.chunk(chunk.getBuffer(), texManager, container);

                // Create mesh
                Mesh mesh = new Mesh();

                //Set coordinates
                Vector3f[] vector3fs = new Vector3f[container.getVector3fs().toArray().length];
                container.getVector3fs().toArray(vector3fs);
                mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vector3fs));
                //Connects triangles
                Integer[] integers = new Integer[container.getIndices().toArray().length];
                container.getIndices().toArray(integers);
                int[] indices = new int[container.getIndices().size()];
                for (int i = 0; i < container.getIndices().size(); i++) {
                    indices[i] = integers[i];
                }
                mesh.setBuffer(Type.Index, 2, BufferUtils.createIntBuffer(indices));

                //Set texture scale and type
                Vector3f[] vector2fs = new Vector3f[container.getTextureCoordinates().toArray().length];
                container.getTextureCoordinates().toArray(vector2fs);
                mesh.setBuffer(Type.TexCoord, 3, BufferUtils.createFloatBuffer(vector2fs));

                //Normals
                Vector3f[] norm = new Vector3f[container.getNormals().toArray().length];
                container.getNormals().toArray(norm);
                mesh.setBuffer(Type.Normal,3,BufferUtils.createFloatBuffer(norm));

                // Create geometry
                Geometry geom = new Geometry("chunk:" + x + "," + y + "," + z, mesh);

                // Create material
                geom.setMaterial(mat);

                //Set chunk position in world
                geom.setLocalTranslation(x, y, z);
                geom.setCullHint(CullHint.Never);

                container.clear();

                // Place geometry in queue for main thread
                geomCreateQueue.add(geom);
            }
        });

        // Some config options
        flyCam.setMoveSpeed(10);
        rootNode.setCullHint(CullHint.Never);

        List<CompletableFuture<Void>> markers = world.updateLoadMarkers();
        markers.forEach(CompletableFuture::join);

        inputManager.addMapping("RELOAD", new KeyTrigger(KeyInput.KEY_G));
        inputManager.addListener(this, "RELOAD");
        inputManager.addMapping("toggle wireframe", new KeyTrigger(KeyInput.KEY_T));
        inputManager.addListener(this, "toggle wireframe");

        rootNode.addLight(new AmbientLight());
    }

    @Override
    public void simpleUpdate(float tpf) {
     /*   loadMarkersUpdated += tpf;
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
        }*/

        while (!geomCreateQueue.isEmpty()) {
            Geometry geom = geomCreateQueue.poll();

            ArrayList<Geometry> geometries = new ArrayList<>();
            for(Spatial spatial:rootNode.getChildren()) {
                if(spatial instanceof Geometry){
                    geometries.add((Geometry) spatial);
                }
            }

            geometries.add(geom);
            Mesh terrain = new Mesh();
            GeometryBatchFactory.mergeGeometries(geometries,  terrain);
            //System.out.println("create geom: " + geom.getLocalTranslation());
            // Create geometry
            rootNode.detachAllChildren();

            Geometry terrainGeom = new Geometry("Map");
            terrainGeom.setMesh(terrain);
            terrainGeom.setMaterial(mat);
            geom.setLocalTranslation(0, 0, 0);
            geom.setCullHint(CullHint.Never);

            rootNode.attachChild(geom);
        }
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals("RELOAD") && isPressed) {
            rootNode.detachAllChildren();
            world.updateLoadMarkers();
        }
        if (name.equals("toggle wireframe") && !isPressed) {
            wireframe = !wireframe; // toggle boolean
            mat.getAdditionalRenderState().setWireframe(wireframe);
        }
    }
}