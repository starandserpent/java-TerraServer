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

import java.nio.IntBuffer;
import java.util.Collection;

import org.lwjgl.BufferUtils;

/**
 * Uses OpenGL hardware occlusion queries to potentially reduce amount of
 * stuff that is rendered.
 * 
 * TODO
 *
 */
public class OcclusionQueryProcessor implements SceneProcessor {
    
    private Collection<VisualObject> objs;
    private Object2IntMap<VisualObject> queries;
    
    /**
     * Constructs a new occlusion query scene processor.
     * Be sure to not modify parameter collection asynchronously!
     * @param objs Mutable collection of visual objects.
     */
    public OcclusionQueryProcessor(Collection<VisualObject> objs) {
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
        // All the hard work is done here
        
        // Make sure that our query objects are not actually rendered
        glColorMask(false, false, false, false); // This also increases performance
        glDepthMask(false);
        
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, BufferUtils.createFloatBuffer(100), GL_STATIC_DRAW);
        
        // First iteration: use the queries from previous frame
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
        

        // Second iteration: do queries for next frame
        for (VisualObject obj : objs) {
            int queryId = glGenQueries();
            
            glBeginQuery(GL_SAMPLES_PASSED, queryId);
            
            glDrawArrays(GL_TRIANGLES, 0, 36);
            
            glEndQuery(GL_SAMPLES_PASSED);
            queries.put(obj, queryId);
        }
        
        // Re-enable normal rendering
        glColorMask(true, true, true, true);
        glDepthMask(true);
    }

    @Override
    public void postQueue(RenderQueue rq) {
        // We cannot remove objects at this stage, so do nothing
    }

    @Override
    public void postFrame(FrameBuffer out) {
        // It is rendered, no use for culling anymore
    }

    @Override
    public void cleanup() {
        // We don't have resources that need cleanup
    }

}
