package com.ritualsoftheold.terra.mesher.test;

/*
public class TextureManagerTest extends SimpleApplication {

    public static void main(String... args) {
        new TextureManagerTest().start();
    }
    
    @Override
    public void simpleInitApp() {
        TextureManager manager = new TextureManager(assetManager); // jME provides asset manager
        MaterialRegistry registry = new MaterialRegistry();
        
        TerraModule mod = new TerraModule("test");
        mod.newMaterial().name("grass").texture(new TerraTexture(32, 32, "grass.png"));
        mod.newMaterial().name("dirt").texture(new TerraTexture(32, 32, "dirt.png"));
        //mod.newMaterial().name("dirt-256").texture(new TerraTexture(32, 32, "NorthenForestDirt256px.png"));
        mod.registerMaterials(registry);
        
        manager.loadMaterials(registry);
        try {
            ImageUtil.writeImage(manager.getAtlas(0), new File("exporttest.png"));
            //writeImageFile(new File("exporttest.png"), assetManager.loadTexture("NorthenForestDirt256px.png").getImage().getData(0), 256, 256);
            //savePng(new File("atlastest.png"), manager.getAtlas(0));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // Used to test texture atlas generation - see https://hub.jmonkeyengine.org/t/how-to-save-texture-as-an-image/34823/3
    public void savePng(File f, Image img) throws IOException {
        OutputStream out = new FileOutputStream(f);
        try {            
            JmeSystem.writeImageFile(out, "png", img.getData(0), img.getWidth(), img.getHeight());  
        } finally {
            out.close();
        }             
    }
    
    protected void writeImageFile(File file, ByteBuffer outBuf, int width, int height) throws IOException {
        OutputStream outStream = new FileOutputStream(file);
        System.out.println(outBuf.capacity());
        try {
            JmeSystem.writeImageFile(outStream, "png", outBuf, width, height);
        } finally {
            outStream.close();
        }
    } 
}
*/