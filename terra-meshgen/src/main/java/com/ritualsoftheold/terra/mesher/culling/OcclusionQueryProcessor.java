package com.ritualsoftheold.terra.mesher.culling;

import com.jme3.post.SceneProcessor;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.texture.FrameBuffer;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;

/**
 * Uses OpenGL hardware occlusion queries to potentially reduce amount of
 * stuff that is rendered.
 * 
 * TODO
 *
 */
public class OcclusionQueryProcessor implements SceneProcessor {
    
    private List<VisualObject> objs;
    private Object2IntMap<VisualObject> queries;
    
    /**
     * Constructs a new occlusion query scene processor.
     * Be sure to not modify parameter list asynchronously!
     * @param objs Mutable list of visual objects.
     */
    public OcclusionQueryProcessor(List<VisualObject> objs) {
        this.objs = objs;
        this.queries = new Object2IntOpenHashMap<>();
    }

    @Override
    public void initialize(RenderManager rm, ViewPort vp) {
        
    }

    @Override
    public void reshape(ViewPort vp, int w, int h) {
        // No need to do anything here
    }

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Override
    public void preFrame(float tpf) {
        // Mark objects which are not to be rendered according to queries

        for (VisualObject obj : objs) {
            int queryId = queries.getInt(obj);
            if (glGetQueryObjecti(queryId, GL_QUERY_RESULT_AVAILABLE) != GL_FALSE) {
                int result = glGetQueryi(queryId, GL_QUERY_RESULT);
                if (result == GL_FALSE) {
                    obj.linkedGeom.setCullHint(CullHint.Always);
                }
            } else { // We better render this stuff, as we have no idea if it is needed or not
                obj.linkedGeom.setCullHint(CullHint.Never);
            }
        }
    }

    @Override
    public void postQueue(RenderQueue rq) {
        // We cannot remove objects at this stage, so do nothing
    }

    @Override
    public void postFrame(FrameBuffer out) {
        // Do queries for next frame
        
        // Make sure that our query objects are not actually rendered
        glColorMask(false, false, false, false); // This also increases performance
        glDepthMask(false);
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer vertices = stack.mallocFloat(36 * objs.size());
            
            int i = 0; // Use separate counter as objs could be LinkedList (get is slow, iterating fast)
            for (VisualObject obj : objs) {
                float posX = obj.posX;
                float posY = obj.posY;
                float posZ = obj.posZ;
                float posMod = obj.posMod;
                
                // LEFT
                vertices.put(posX - posMod).put(posY - posMod).put(posZ - posMod)
                .put(posX - posMod).put(posY + posMod).put(posZ - posMod)
                .put(posX - posMod).put(posY + posMod).put(posZ + posMod)
                
                .put(posX - posMod).put(posY + posMod).put(posZ + posMod)
                .put(posX - posMod).put(posY - posMod).put(posZ + posMod)
                .put(posX - posMod).put(posY - posMod).put(posZ - posMod);
                
                // RIGHT
                vertices.put(posX + posMod).put(posY + posMod).put(posZ + posMod)
                .put(posX + posMod).put(posY + posMod).put(posZ - posMod)
                .put(posX + posMod).put(posY - posMod).put(posZ - posMod)
                
                .put(posX + posMod).put(posY - posMod).put(posZ - posMod)
                .put(posX + posMod).put(posY - posMod).put(posZ + posMod)
                .put(posX + posMod).put(posY + posMod).put(posZ + posMod);
                
                // TODO rest of vertices
                
                
            }
            vertices.flip(); // Needed by LWJGL to not crash horribly
            
            i = 0; // Reset the counter
            for (VisualObject obj : objs) {
                int queryId = glGenQueries();
                
                glBeginQuery(GL_SAMPLES_PASSED, queryId);
                
                glDrawArrays(GL_TRIANGLES, 36 * i, 36);
                
                glEndQuery(GL_SAMPLES_PASSED);
                queries.put(obj, queryId);
                
                i++;
            }
        }
        
        // Re-enable normal rendering
        glColorMask(true, true, true, true);
        glDepthMask(true);
    }

    @Override
    public void cleanup() {
        // We don't have resources that need cleanup
    }

}
