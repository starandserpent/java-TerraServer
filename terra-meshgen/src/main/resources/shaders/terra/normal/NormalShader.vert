uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldMatrix;

attribute vec3 inPosition;
attribute vec3 inNormal;

in vec3 inTexCoord;
out vec3 texCoord;
out vec3 normal;

void main() {
  texCoord = inTexCoord;
  normal = inNormal;

  // Calculate real position
  gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1.0); // Position for fragment shader
}
