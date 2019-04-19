uniform mat4 g_WorldViewProjectionMatrix;

attribute vec3 inPosition;

in vec2 inTexCoord;

out vec2 texCoord;

void main() {
  // Calculate real position
  texCoord = inTexCoord;

  gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition.x,  inPosition.y, inPosition.z, 1.0); // Position for fragment shader
}
