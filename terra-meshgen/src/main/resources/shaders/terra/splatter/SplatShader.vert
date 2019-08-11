#import "shaders/terra/splatter/SplatShaderLib.glsllib"

uniform mat4 g_WorldMatrix;
uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldViewMatrix;
uniform mat4 g_ViewProjectionMatrix;
uniform mat4 g_ProjectionMatrix;
uniform mat4 g_ViewMatrix;
uniform vec2 g_Resolution;
uniform vec3 g_CameraPosition;
uniform vec3 g_CameraDirection;

#ifdef HAS_VOXELSIZE
    uniform float m_VoxelSize;
#endif

attribute vec3 inPosition;
attribute vec4 inColor;

varying vec4 vertColor;
varying Box voxelBox;

void main() {
    float padding = 2.0f;
    vertColor = inColor;
    float shaderVoxSize = m_VoxelSize *2.0f;

    gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1.0);
    gl_PointSize = g_Resolution.y * g_ProjectionMatrix[1][1] * (shaderVoxSize * padding) / gl_Position.w;

    vec4 Position = g_WorldMatrix * vec4(inPosition,1.0);
    vec3 min_pos_Box = Position.xyz - vec3(shaderVoxSize);
    vec3 max_pos_Box = Position.xyz + vec3(shaderVoxSize);

    voxelBox = Box(Position.xyz,vec3(shaderVoxSize),1.0/vec3(shaderVoxSize),mat3(vec3(1.0,0.0,0.0),
                                                                        vec3(0.0,1.0,0.0),
                                                                        vec3(0.0,0.0,1.0)),min_pos_Box,max_pos_Box);
}
