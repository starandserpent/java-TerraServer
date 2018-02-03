uniform mat4 g_WorldViewProjectionMatrix;

in vec3 inPosition;

// Packed texture coordinates and tile index
in float inTexCoord;

out vec3 texCoord;
out flat float tile;

void main() {
  gl_Position = g_WorldViewProjectionMatrix * vec4(inPosition, 1.0); // Position for fragment shader

  // Calculate real texture coordinates
  uint texBits = floatBitsToUint(inTexCoord);
  tile = float(texBits >> 24);
  float x = float(texBits >> 16 & 0xff);
  float y = float(texBits >> 8 & 0xff);
  float z = float(texBits & 0xff); // Texture array index, not normalized

  texCoord = vec3(1024f / x, 1024f / y, z); // Write outgoing texCoord for frag shader
}
