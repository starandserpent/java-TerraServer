package com.ritualsoftheold.terra.mesher.test;

//import com.ritualsoftheold.terra.offheap.chunk.iterator.ChunkIterator;

/**
 * Simplistic chunk mesher test.
 *
 */
/*
public class ChunkTest extends SimpleApplication {
    
    private static final Memory mem = OS.memory();
    
    public static void main(String... args) {
        ChunkTest app = new ChunkTest();
        app.setShowSettings(false);
        
        AppSettings settings = new AppSettings(true);
        settings.setResolution(1024, 768);
        settings.setVSync(true);
        app.settings = settings;
        
        app.start();
    }
    
    private PointLight light;

    @Override
    public void simpleInitApp() {
        //setDisplayFps(false);
        //setDisplayStatView(false);
        viewPort.setBackgroundColor(ColorRGBA.White); // Easier to debug
        
        // Create chunk data
        long addr = mem.allocate(DataConstants.CHUNK_UNCOMPRESSED);
        mem.setMemory(addr, DataConstants.CHUNK_UNCOMPRESSED, (byte) 0);
        mem.writeShort(addr, (short) 1); // Add some stuff to chunk
        mem.writeShort(addr + 2, (short) 0xffff);
        mem.writeShort(addr + 4, (short) 0);
        mem.writeShort(addr + 6, (short) 0xffff);
        mem.writeShort(addr + 8, (short) 0);
        mem.writeShort(addr + 10, (short) 0xffff);
        mem.writeShort(addr + 12, (short) 0);
        mem.writeShort(addr + 14, (short) 0xffff);
        mem.writeShort(addr + 16, (short) 0);
        mem.writeShort(addr + 18, (short) 0xffff);
        mem.writeShort(addr + 20, (short) 0);
        mem.writeShort(addr + 22, (short) 0xffff);
                
        // Register materials
        TextureManager manager = new TextureManager(assetManager); // jME provides asset manager
        MaterialRegistry registry = new MaterialRegistry();
        
        TerraModule mod = new TerraModule("test");
//        mod.newMaterial().name("grass").texture(new TerraTexture(32, 32, "grass.png"));
//        mod.newMaterial().name("dirt").texture(new TerraTexture(32, 32, "dirt.png"));
        mod.newMaterial().name("arrow").texture(new TerraTexture(128, 128, 0.25f, "arrow.png"));
        //mod.newMaterial().name("dirt-256").texture(new TerraTexture(256, 256, 0.25f, "NorthenForestDirt256px.png"));
        mod.registerMaterials(registry);
        
        manager.loadMaterials(registry);
        
        VoxelMesher mesher = new NaiveGreedyMesher(); // Create mesher
        MeshContainer meshContainer = new MeshContainer(100, ByteBufAllocator.DEFAULT);
        //mesher.chunk(ChunkIterator.forChunk(addr, ChunkType.RLE_2_2), manager, meshContainer);
        
        // Create mesh
        Mesh mesh = new Mesh();
        //System.out.println(mesher.getVertices());
        //System.out.println(mesher.getIndices());
        mesh.setBuffer(Type.Position, 1, meshContainer.getVertices().nioBuffer().asFloatBuffer());
        mesh.setBuffer(Type.Index, 3, meshContainer.getIndices().nioBuffer().asIntBuffer());
        mesh.setBuffer(Type.TexCoord, 2, meshContainer.getTextureCoordinates().nioBuffer().asFloatBuffer());
        
        // Create geometry
        Geometry geom = new Geometry("test_chunk", mesh);
        Material mat = new Material(assetManager, "shaders/terra/NormalShader.j3md");
//        mat.getAdditionalRenderState().setWireframe(true);
//        mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        mat.setTexture("ColorMap", manager.getGroundTexture());
        //mat.setParam("SeparateTexCoord", VarType.Boolean, true);
        geom.setMaterial(mat);
        //geom.setLocalScale(0.5f);
        geom.setCullHint(CullHint.Never);
        rootNode.attachChild(geom);
        flyCam.setMoveSpeed(10);
        rootNode.setCullHint(CullHint.Never);
        
        light = new PointLight();
        light.setRadius(400);
        light.setPosition(cam.getLocation());
        rootNode.addLight(light);
        
//        OcclusionQueryProcessor queryProcessor = new OcclusionQueryProcessor(1, 1, assetManager);
//        VisualObject obj = new VisualObject();
//        obj.linkedGeom = geom;
//        obj.posMod = 8;
//        obj.pos = geom.getLocalTranslation();
//        queryProcessor.addObject(obj);
//        
//        viewPort.addProcessor(queryProcessor);
    }
    
    @Override
    public void simpleUpdate(float tpf) {
        //light.setPosition(cam.getLocation());
    }
*/
