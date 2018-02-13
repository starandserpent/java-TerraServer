package com.ritualsoftheold.terra.mesher.test;

import com.jme3.app.SimpleApplication;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.system.AppSettings;
import com.jme3.util.BufferUtils;
import com.ritualsoftheold.terra.TerraModule;
import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.material.TerraTexture;
import com.ritualsoftheold.terra.mesher.MeshContainer;
import com.ritualsoftheold.terra.mesher.NaiveMesher;
import com.ritualsoftheold.terra.mesher.VoxelMesher;
import com.ritualsoftheold.terra.mesher.culling.OcclusionQueryProcessor;
import com.ritualsoftheold.terra.mesher.culling.VisualObject;
import com.ritualsoftheold.terra.mesher.resource.TextureManager;
import com.ritualsoftheold.terra.offheap.DataConstants;
import com.ritualsoftheold.terra.offheap.chunk.ChunkType;
import com.ritualsoftheold.terra.offheap.chunk.iterator.ChunkIterator;

import io.netty.buffer.ByteBufAllocator;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;

/**
 * Simplistic chunk mesher test.
 *
 */
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
        mem.writeShort(addr + 2, (short) 0xfff);
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
        //System.out.println(Long.toBinaryString(mem.readLong(addr)));
        
        // Register materials
        TextureManager manager = new TextureManager(assetManager); // jME provides asset manager
        MaterialRegistry registry = new MaterialRegistry();
        
        TerraModule mod = new TerraModule("test");
//        mod.newMaterial().name("grass").texture(new TerraTexture(32, 32, "grass.png"));
//        mod.newMaterial().name("dirt").texture(new TerraTexture(32, 32, "dirt.png"));
        mod.newMaterial().name("arrow").texture(new TerraTexture(128, 128, "arrow.png"));
        //mod.newMaterial().name("dirt-256").texture(new TerraTexture(32, 32, "NorthenForestDirt256px.png"));
        mod.registerMaterials(registry);
        
        manager.loadMaterials(registry);
        
        VoxelMesher mesher = new NaiveMesher(); // Create mesher
        MeshContainer meshContainer = new MeshContainer(100, ByteBufAllocator.DEFAULT);
        mesher.chunk(ChunkIterator.forChunk(addr, ChunkType.RLE_2_2), manager, meshContainer);
        
        // Create mesh
        Mesh mesh = new Mesh();
        //System.out.println(mesher.getVertices());
        //System.out.println(mesher.getIndices());
        mesh.setBuffer(Type.Position, 1, meshContainer.getVertices().nioBuffer().asFloatBuffer());
        mesh.setBuffer(Type.Index, 3, meshContainer.getIndices().nioBuffer().asIntBuffer());
        mesh.setBuffer(Type.TexCoord, 2, meshContainer.getTextureCoordinates().nioBuffer().asFloatBuffer());
        
        // Create geometry
        Geometry geom = new Geometry("test_chunk", mesh);
        Material mat = new Material(assetManager, "terra/shader/TerraArray.j3md");
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
}
