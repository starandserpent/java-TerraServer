package com.ritualsoftheold.terra.test;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;
import com.ritualsoftheold.terra.core.TerraModule;
import com.ritualsoftheold.terra.core.material.MaterialRegistry;
import com.ritualsoftheold.terra.core.material.TerraMaterial;
import com.ritualsoftheold.terra.core.material.TerraTexture;
import com.ritualsoftheold.terra.mesher.GreedyMesher;
import com.ritualsoftheold.terra.mesher.MeshContainer;
import com.ritualsoftheold.terra.mesher.resource.TextureManager;
import com.ritualsoftheold.terra.world.test.DummyPalette16ChunkBuffer;
import com.ritualsoftheold.terra.world.test.DummyTextureManager;
import com.ritualsoftheold.terra.world.test.DummyWorldGenerator;
import jme3tools.optimize.TextureAtlas;

public class GreedyMesherTestApp extends SimpleApplication {

    public static void main(String... args) {
        GreedyMesherTestApp app = new GreedyMesherTestApp();
        app.showSettings = false;
        app.settings = new AppSettings(true);
        app.settings.setResolution(1600, 900);
        app.settings.setTitle("Terra testgame");
        app.settings.setFullscreen(false);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        TextureManager texManager = new TextureManager(assetManager);
        GreedyMesher greedyMesher = new GreedyMesher();
        MeshContainer container = new MeshContainer();

        TerraModule mod = new TerraModule("testgame");
        mod.newMaterial().name("dirt").texture(new TerraTexture(256, 256, "NorthenForestDirt256px.png"));
        mod.newMaterial().name("grass").texture(new TerraTexture(256, 256, "NorthenForestGrass256px.png"));
        MaterialRegistry reg = new MaterialRegistry();
        mod.registerMaterials(reg);

        DummyWorldGenerator worldGenerator = new DummyWorldGenerator(reg);
        greedyMesher.chunk(worldGenerator.generate(new DummyPalette16ChunkBuffer(reg)), texManager, container);

        TerraMaterial dirt = reg.getMaterial("testgame:dirt");
        TerraMaterial grass = reg.getMaterial("testgame:grass");
        TerraMaterial air = reg.getMaterial("base:air");

        // Create mesh
        Mesh mesh = new Mesh();

        Vector3f[] vector3fs = new Vector3f[container.getVector3fs().toArray().length];
        container.getVector3fs().toArray(vector3fs);

        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vector3fs));

        Integer[] integers = new Integer[container.getIndices().toArray().length];
        container.getIndices().toArray(integers);

        int[] indices = new int[container.getIndices().size()];
        for (int i = 0; i < container.getIndices().size(); i++) {
            indices[i] = integers[i];
        }

        mesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(indices));

        Vector2f[] vector2fs = new Vector2f[container.getTextureCoordinates().toArray().length];
        container.getTextureCoordinates().toArray(vector2fs);

        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(vector2fs));
        mesh.updateBound();

        // Create geometry
        Geometry geom = new Geometry("chunk:", mesh);

        // Create material
        DummyTextureManager dummyTextureManager = new DummyTextureManager(reg);
        Texture texture = dummyTextureManager.convertTexture(assetManager);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");

      //  mat.getAdditionalRenderState().setWireframe(true);

        // create manually texture atlas by adding textures or geometries with textures
        //create material and set texture
        mat.setTexture("ColorMap", texture);
      //  mat.setColor("Color", ColorRGBA.Blue);
        //change one geometry to use atlas, apply texture coordinates and replace material.
        geom.setMaterial(mat);

        geom.setLocalTranslation(0, 0, 0);
        geom.setCullHint(Spatial.CullHint.Never);

        // Place geometry in queue for main thread
        rootNode.attachChild(geom);
    }
}
