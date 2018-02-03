uniform mat4 g_WorldViewProjectionMatrix;

in vec3 inPosition;

// 10 bits per axis, each coordinate represents 1/1024th of texture
in float inTexCoord;

out vec3 texCoord;

void main() {
  gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1.0); // Position for fragment shader

  // Calculate real texture coordinates
  uint texBits = floatBitsToUint(inTexCoord);
  float x = float(texBits >> 22);
  float y = float(texBits >> 12 & 0x3ff);
  float z = float(texBits & 0x3ff); // Texture array index, not normalized

  texCoord = vec3(1024f / x, 1024f / y, z); // Write outgoing texCoord for frag shader
}
