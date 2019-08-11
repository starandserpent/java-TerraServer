uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldMatrix;

attribute vec3 inPosition;
attribute vec3 inNormal;

out vec2 texCoord;
out float dist;

void main() {
  texCoord = vec2(inPosition.xy);
  dist = -(g_WorldViewProjectionMatrix * vec4(inPosition, 1.0)).z;

  // Calculate real position
  gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1.0); // Position for fragment shader
}
