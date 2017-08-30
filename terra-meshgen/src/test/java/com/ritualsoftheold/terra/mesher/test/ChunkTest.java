package com.ritualsoftheold.terra.mesher.test;

import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.shader.VarType;
import com.jme3.system.AppSettings;
import com.jme3.util.BufferUtils;
import com.ritualsoftheold.terra.TerraModule;
import com.ritualsoftheold.terra.material.MaterialRegistry;
import com.ritualsoftheold.terra.material.TerraTexture;
import com.ritualsoftheold.terra.mesher.NaiveMesher;
import com.ritualsoftheold.terra.mesher.VoxelMesher;
import com.ritualsoftheold.terra.mesher.culling.OcclusionQueryProcessor;
import com.ritualsoftheold.terra.mesher.culling.VisualObject;
import com.ritualsoftheold.terra.mesher.resource.TextureManager;
import com.ritualsoftheold.terra.offheap.DataConstants;

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
        settings.setVSync(true);
        app.settings = settings;
        
        app.start();
    }
    
    private PointLight light;

    @Override
    public void simpleInitApp() {
        setDisplayFps(false);
        setDisplayStatView(false);
        
        // Create chunk data
        long addr = mem.allocate(DataConstants.CHUNK_UNCOMPRESSED);
        mem.setMemory(addr, DataConstants.CHUNK_UNCOMPRESSED, (byte) 0);
        mem.writeByte(addr, (byte) 0); // Chunk type here
        mem.writeShort(addr + 1, (short) 2); // Add some stuff to chunk
        mem.writeShort(addr + 3, (short) 0xffff);
        mem.writeShort(addr + 5, (short) 1);
        mem.writeShort(addr + 7, (short) 0xffff);
        mem.writeShort(addr + 9, (short) 1);
        mem.writeShort(addr + 11, (short) 0xffff);
        mem.writeShort(addr + 13, (short) 1);
        mem.writeShort(addr + 15, (short) 0xffff);
        //System.out.println(Long.toBinaryString(mem.readLong(addr)));
        
        // Register materials
        TextureManager manager = new TextureManager(assetManager); // jME provides asset manager
        MaterialRegistry registry = new MaterialRegistry();
        
        TerraModule mod = new TerraModule("test");
        mod.newMaterial().name("grass").texture(new TerraTexture(32, 32, "grass.png"));
        mod.newMaterial().name("dirt").texture(new TerraTexture(32, 32, "dirt.png"));
        //mod.newMaterial().name("dirt-256").texture(new TerraTexture(32, 32, "NorthenForestDirt256px.png"));
        mod.registerMaterials(registry);
        
        manager.loadMaterials(registry);
        
        VoxelMesher mesher = new NaiveMesher(); // Create mesher
        mesher.chunk(addr, manager); // TODO check back when material registry is done
        
        // Create mesh
        Mesh mesh = new Mesh();
        //System.out.println(mesher.getVertices());
        //System.out.println(mesher.getIndices());
        mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(mesher.getVertices().toFloatArray()));
        mesh.setBuffer(Type.Index, 3, BufferUtils.createIntBuffer(mesher.getIndices().toIntArray()));
        mesh.setBuffer(Type.TexCoord, 3, BufferUtils.createFloatBuffer(mesher.getTextureCoords().toFloatArray()));
        
        // Create geometry
        Geometry geom = new Geometry("test_chunk", mesh);
        Material mat = new Material(assetManager, "terra/shader/TerraArray.j3md");
        //mat.getAdditionalRenderState().setWireframe(true);
        //mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        mat.setTexture("DiffuseMap", manager.getGroundTexture());
        //mat.setParam("SeparateTexCoord", VarType.Boolean, true);
        geom.setMaterial(mat);
        //geom.setLocalScale(0.5f);
        geom.setCullHint(CullHint.Never);
        rootNode.attachChild(geom);
        flyCam.setMoveSpeed(10);
        rootNode.setCullHint(CullHint.Never);
        
        light = new PointLight();
        light.setRadius(400);
        rootNode.addLight(light);
        
        OcclusionQueryProcessor queryProcessor = new OcclusionQueryProcessor(1, 1);
        VisualObject obj = new VisualObject();
        obj.linkedGeom = geom;
        obj.posMod = 8;
        queryProcessor.addObject(obj);
        
        viewPort.addProcessor(queryProcessor);
    }
    
    @Override
    public void simpleUpdate(float tpf) {
        light.setPosition(cam.getLocation());
    }
}
