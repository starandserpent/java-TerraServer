uniform mat4 g_WorldViewProjectionMatrix;

in float inPosition;

// Packed texture coordinates and tile index
in float inTexCoord;

out vec3 texCoord;
out flat float tile;

void main() {
  // Calculate real position
  uint posBits = floatBitsToUint(inPosition);
  float posX = float(posBits >> 22);
  float posY = float(posBits >> 12 & 0x3ff);
  float posZ = float(postBits & 0x3ff);
  gl_Position = g_WorldViewProjectionMatrix * vec4(posX * 0.015625f, posY * 0.015625f, posZ * 0.015625f, 1.0); // Position for fragment shader

  // Calculate real texture coordinates
  uint texBits = floatBitsToUint(inTexCoord);
  tile = float(texBits >> 24);
  float texX = float(texBits >> 16 & 0xff);
  float texY = float(texBits >> 8 & 0xff);
  float texZ = float(texBits & 0xff); // Texture array index, not normalized

  texCoord = vec3(1024f / texX, 1024f / texY, texZ); // Write outgoing texCoord for frag shader
}
