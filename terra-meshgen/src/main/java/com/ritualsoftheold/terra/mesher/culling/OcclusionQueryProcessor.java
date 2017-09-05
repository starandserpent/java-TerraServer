package com.ritualsoftheold.terra.mesher.culling;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.post.Filter;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.RenderContext;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Box;
import com.jme3.texture.FrameBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL33.*;


/**
 * Uses OpenGL hardware occlusion queries to potentially reduce amount of
 * stuff that is rendered.
 *
 */
public class OcclusionQueryProcessor implements SceneProcessor {
    
    private VisualObject[] objs;
    private int objCount;
    private int extraAlloc;
    
    private RenderState queryState;
    private Material queryMat;
    
    private RenderManager renderManager;
    
    /**
     * Constructs a new occlusion query scene processor.
     * @param initialCount How many visual objects there will be initially.
     * Must be at least 0.
     * @param extraAlloc How much array space will be allocated whenever
     * increasing array size. Must be at least 1!
     * @param assetManager jME asset manager.
     */
    public OcclusionQueryProcessor(int initialCount, int extraAlloc, AssetManager assetManager) {
        if (extraAlloc < 1) {
            throw new IllegalArgumentException("extraAlloc must be at least");
        } else if (initialCount < 0) {
            throw new IllegalArgumentException("initialCount cannot be negative");
        }
        
        this.objs = new VisualObject[initialCount + extraAlloc];
        this.objCount = 0;
        this.extraAlloc = extraAlloc;
        
        // Initialize query rendering context
        queryMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        this.queryState = queryMat.getAdditionalRenderState();
        queryState.setDepthTest(true);
        queryState.setDepthWrite(false);
        queryState.setColorWrite(false);
    }
    
    /**
     * Adds visual object to this query processor.
     * @param obj Object.
     * @return Index of the object.
     */
    public int addObject(VisualObject obj) {
        if (objs.length == objCount) {
            // Need to allocate more array
            VisualObject[] old = objs;
            objs = new VisualObject[objCount + extraAlloc];
            System.arraycopy(old, 0, objs, 0, objCount);
        }
        objs[objCount] = obj;
        objCount++;
        
        return objCount - 1;
    }
    
    public void removeObject(int index) {
        objs[index] = null;
    }
    
    public void removeObject(VisualObject obj) {
        removeObject(obj.cullingId);
    }
    
    @Override
    public void initialize(RenderManager rm, ViewPort vp) {
        renderManager = rm;
    }
    
    @Override
    public boolean isInitialized() {
        return renderManager != null;
    }

    @Override
    public void reshape(ViewPort vp, int w, int h) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void cleanup() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void preFrame(float tpf) {
        // Do nothing...
    }

    @Override
    public void postQueue(RenderQueue rq) {
        // We cannot remove objects at this stage, so do nothing
    }

    @Override
    public void postFrame(FrameBuffer out) {
        // Do queries
        Renderer renderer = renderManager.getRenderer();
        
        // Create query ids
        int[] queries = new int[objCount];
        glGenQueries(queries);
            
        // Begin the queries
        for (int i = 0; i < objCount; i++) {
            VisualObject obj = objs[i];
            if (obj == null) {
                continue;
            }
            
            int queryId = queries[i]; // Just pick one query id based on index
            
            Geometry box = obj.boundingBox;
            if (box == null) {
                // Sorry jME, I'm not confused (see Javadoc of Box)
                Box boxMesh = new Box(obj.posMod, obj.posMod, obj.posMod);
                box = new Geometry("culling box");
                box.setMesh(boxMesh);
                box.setLocalTranslation(obj.pos);
                obj.boundingBox = box;
            }
            
            glBeginQuery(GL_SAMPLES_PASSED, queryId);
            
            queryMat.render(box, renderManager);
            
            glEndQuery(GL_SAMPLES_PASSED);
        }
        
        // Mark objects which are not to be rendered according to queries
        for (int i = 0; i < objCount; i++) {
            VisualObject obj = objs[i];
            
            if (obj == null) { // Ignore null objects...
                continue;
            }
            
            int queryId = queries[i];
            
            // Check if query is available...
            if (glGetQueryObjectui(queryId, GL_QUERY_RESULT_AVAILABLE) == GL_FALSE) {
                // It is, check if it is visible
                int result = glGetQueryObjectui(queryId, GL_QUERY_RESULT);
                if (result == GL_FALSE) { // Nope
                    System.out.println("GL_FALSE");
                    obj.linkedGeom.setCullHint(CullHint.Always);
                } else { // Yeah, visible
                    System.out.println("GL_TRUE: " + result);
                    obj.linkedGeom.setCullHint(CullHint.Never);
                }
            } else { // We better render this stuff, as we have no idea if it is needed or not
                obj.linkedGeom.setCullHint(CullHint.Never);
            }
        }
        
        // Close all queries to avoid flickering
        glDeleteQueries(queries);
    }

    @Override
    public void setProfiler(AppProfiler profiler) {
        // TODO Auto-generated method stub
        
    }
}
