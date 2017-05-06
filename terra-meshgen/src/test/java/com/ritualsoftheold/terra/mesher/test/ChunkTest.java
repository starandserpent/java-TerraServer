package com.ritualsoftheold.terra.mesher.test;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import com.ritualsoftheold.terra.mesher.NaiveMesher;
import com.ritualsoftheold.terra.mesher.VoxelMesher;
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
        new ChunkTest().start();
    }

    @Override
    public void simpleInitApp() {
        // Create chunk data
        long addr = mem.allocate(DataConstants.CHUNK_UNCOMPRESSED);
        mem.setMemory(addr, DataConstants.CHUNK_UNCOMPRESSED, (byte) 0);
        mem.writeShort(addr, (short) 1); // Add some stuff to chunk
        //mem.writeShort(addr + 8192, (short) 0xffff);
        System.out.println("addr: " + addr);
        for (int i = 2; i < 20000; i += 2) {
            mem.writeShort(addr + i, (short) 0xffff);
        }
        System.out.println(Long.toBinaryString(mem.readLong(addr)));
        
        VoxelMesher mesher = new NaiveMesher(); // Create mesher
        mesher.chunk(addr, null); // TODO check back when material registry is done
        
        // Create mesh
        Mesh mesh = new Mesh();
        //System.out.println(mesher.getVertices());
        //System.out.println(mesher.getIndices());
        mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(mesher.getVertices().toFloatArray()));
        mesh.setBuffer(Type.Index, 3, BufferUtils.createIntBuffer(mesher.getIndices().toIntArray()));
        mesh.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(mesher.getTextureCoords().toFloatArray()));
        
        // Create geometry
        Geometry geom = new Geometry("test_chunk", mesh);
        Material mat = new Material(assetManager, "jme3test/texture/UnshadedArray.j3md");
        mat.getAdditionalRenderState().setWireframe(true);
        //mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        geom.setMaterial(mat);
        geom.setLocalScale(0.25f);
        geom.setCullHint(CullHint.Never);
        rootNode.attachChild(geom);
        flyCam.setMoveSpeed(10);
    }
}
