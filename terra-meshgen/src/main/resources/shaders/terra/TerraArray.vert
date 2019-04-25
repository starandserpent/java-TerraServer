uniform mat4 g_WorldViewProjectionMatrix;

attribute vec3 inPosition;

in vec3 inTexCoord;
out vec3 texCoord;
out float postiony;

void main() {
  texCoord = inTexCoord;

  postiony = inPosition.y;
  // Calculate real position
  gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1.0); // Position for fragment shader
}
