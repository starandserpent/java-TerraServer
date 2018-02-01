uniform mat4 g_WorldViewProjectionMatrix;

in vec3 inPosition;
void main() {
  gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1.0);
}
