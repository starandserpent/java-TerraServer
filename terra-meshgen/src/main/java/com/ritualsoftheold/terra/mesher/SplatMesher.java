package com.ritualsoftheold.terra.mesher;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.ritualsoftheold.terra.core.buffer.BlockBuffer;
import com.ritualsoftheold.terra.core.material.TerraMaterial;
import com.ritualsoftheold.terra.core.material.TerraTexture;
import com.ritualsoftheold.terra.mesher.resource.TextureManager;
import com.ritualsoftheold.terra.offheap.DataConstants;

public class SplatMesher implements VoxelMesher {
    @Override
    public void chunk(BlockBuffer data, TextureManager textures, MeshContainer mesh) {
        assert data != null;
        assert  textures != null;
        assert  mesh != null;

        data.seek(0);
        int index = 0;
        byte[] voxels = new byte[DataConstants.CHUNK_MAX_BLOCKS];

        while (data.hasNext()){
            TerraTexture terraTexture = data.read().getTexture();
            if(terraTexture == null){
                data.next();
                voxels[index] = -1;
                index+=1;
                continue;
            }
            voxels[index] = 0b00000001;
            index+=1;
            data.next();
        }

        for(index = 0; index < voxels.length; index++){
            //If our voxel is air or something we skip
            if(voxels[index] != -1){
                //Position of current voxel
                int z = index / 4096;
                int y = (index - 4096 * z) / 64;
                int x = index % 64;

                boolean canCreateVoxel = false;

                //Left
                if(x==0 || x > 0 && voxels[index -1] == -1){
                    canCreateVoxel = true;
                }
                //Right
                else if(x == 63 || voxels[index + 1] == -1){
                    canCreateVoxel = true;
                }
                //Top
                else if (y == 63 || voxels[index + 64] == -1){
                    canCreateVoxel = true;
                }
                //Bottom
                else if (y == 0 || voxels[index - 64] == -1){
                    canCreateVoxel = true;
                }
                //Back
                else if(z == 63 || voxels[index + 4096] == -1){
                    canCreateVoxel= true;
                }
                //Front
                else if(z == 0 || voxels[index - 4096] == -1){
                    canCreateVoxel = true;
                }

                if(canCreateVoxel){
                    mesh.vector(new Vector3f(x,y,z));
                    mesh.color(ColorRGBA.randomColor());
                }

            }




        }
    }

    @Override
    public void cube(int id, float scale, TextureManager textures, MeshContainer mesh) {

    }
}
