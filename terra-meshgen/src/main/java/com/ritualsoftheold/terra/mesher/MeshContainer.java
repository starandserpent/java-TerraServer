package com.ritualsoftheold.terra.mesher;

        import com.jme3.math.Vector2f;
        import com.jme3.math.Vector3f;
        import com.ritualsoftheold.terra.core.material.TerraTexture;
        import io.netty.buffer.ByteBuf;
        import io.netty.buffer.ByteBufAllocator;

        import java.util.ArrayList;
        import java.util.HashMap;

/**
 * Helps with building meshes for voxel data.
 *
 */
public class MeshContainer {
    private TerraTexture[][][] textures;

    private HashMap<TerraTexture, Integer> textureTypes;

    ArrayList<Vector3f> vertices;

    ArrayList<Integer> indices;

    ArrayList<Vector2f> texCoords;


    /**
     * Creates a new mesh container.
     */
    public MeshContainer() {
        vertices = new ArrayList<>();
        indices = new ArrayList<>();
        texCoords = new ArrayList<>();
        textures = new TerraTexture[64][64][64];
        textureTypes = new HashMap<>();
    }

    public void vertex(int x, int y, int z) {
        Vector3f vector = new Vector3f(x, y, z);
        vertices.add(vector);
    }

    public void triangle(int vertIndex, int i, int j, int k) {
        indices.add(vertIndex + i);
        indices.add(vertIndex + j);
        indices.add(vertIndex + k);
    }

    public void texture(float x, float y) {
        int nX = (int) x*16;
        int nY = (int) y*16;

        Vector2f vector2f = new Vector2f(nX, nY);
        texCoords.add(vector2f);
    }

    public void setTextures(int nX, int nY, int nZ, TerraTexture texture) {
        textures[nZ][nY][nX] = texture;

        int i = 1;
        if(textureTypes.get(texture) != null) {
            i = textureTypes.get(texture);
            i++;
        }
        textureTypes.put(texture, i);
    }

    public TerraTexture[][][] getTextures(){
        return textures;
    }

    public TerraTexture getMainTexture() {
        int max = 0;
        TerraTexture texture = null;
        for (TerraTexture key : textureTypes.keySet()) {
            if(textureTypes.get(key) == 0) {
                textureTypes.remove(texture);
            } else if (textureTypes.get(key) > max) {
                max = textureTypes.get(key);
                texture = key;
            }
        }

        return texture;
    }

    public int getTextureTypes() {
        return textureTypes.size();
    }

    public ArrayList<Vector3f> getVertices() {
        return vertices;
    }

    public ArrayList<Integer> getIndices() {
        return indices;
    }

    public ArrayList<Vector2f> getTextureCoordinates() {
        return texCoords;
    }
}