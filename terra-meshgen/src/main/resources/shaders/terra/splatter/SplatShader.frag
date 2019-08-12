#import "shaders/terra/splatter/SplatShaderLib.glsllib"
uniform vec3 g_CameraPosition;
uniform mat4 g_ViewProjectionMatrixInverse;
uniform vec4 g_ViewPort;

varying Box voxelBox;
varying vec4 vertColor;


vec2 intersectAABB(vec3 rayOrigin, vec3 rayDir, vec3 boxMin, vec3 boxMax) {
    vec3 tMin = (boxMin - rayOrigin) / rayDir;
    vec3 tMax = (boxMax - rayOrigin) / rayDir;
    vec3 t1 = min(tMin, tMax);
    vec3 t2 = max(tMin, tMax);
    float tNear = max(max(t1.x, t1.y), t1.z);
    float tFar = min(min(t2.x, t2.y), t2.z);
    return vec2(tNear, tFar);
}

void main() {
    vec2 p = 2.0 * vec2(gl_FragCoord.xy) / (g_ViewPort.zw - g_ViewPort.xy) - vec2(1.0);
    vec3 ro = g_CameraPosition;
    vec4 rdh = g_ViewProjectionMatrixInverse * vec4(p, -1.0, 1.0);
    vec3 rd = rdh.xyz/rdh.w - ro;

    vec2 result = intersectAABB(ro,rd,voxelBox.min_pos,voxelBox.max_pos);
    bool rayIntersectionTest = result.y > result.x;

    if(rayIntersectionTest == false){
        //    col = vec3(1.0,1.0,1.0);
        //    return;
        discard;
    }
    vec3 col = vec3(vertColor);
    col = sqrt( col );
    gl_FragColor = vertColor;
}
