package com.ritualsoftheold.terra.mesher.culling;

import com.jme3.post.SceneProcessor;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Uses OpenGL hardware occlusion queries to potentially reduce amount of
 * stuff that is rendered.
 * 
 * TODO
 *
 */
public class OcclusionQueryProcessor implements SceneProcessor {

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
        // All the hard work here
        
        int queryId = glGenQueries();
        
        glBeginQuery(GL_SAMPLES_PASSED, queryId);
        
        // TODO traverse meshes or octree here - not sure how exactly
        
        glEndQuery(queryId);
        
        // Now, prepare to cull some objects
        if (glGetQueryObjecti(queryId, GL_QUERY_RESULT_AVAILABLE) != GL_FALSE) {
            // TODO set object CullHint -> Always
        } // Else: do whatever last query result indicated
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
