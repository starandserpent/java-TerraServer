uniform mat4 g_WorldViewProjectionMatrix;

in float inPosition;

// Packed texture coordinates and tile index
in vec2 inTexCoord;

out vec3 texCoord;
flat out vec2 tile;

void main() {
  // Calculate real position
  uint posBits = floatBitsToUint(inPosition);
  float posX = float(posBits >> 22);
  float posY = float(posBits >> 12 & 0x3ff);
  float posZ = float(posBits & 0x3ff);
  gl_Position = g_WorldViewProjectionMatrix * vec4(posX * 0.25f, posY * 0.25f, posZ * 0.25f, 1.0); // Position for fragment shader

  // Calculate real texture coordinates (normalized)
  uint texBits = floatBitsToUint(inTexCoord.x);
  float texX = float(texBits >> 16 & 0xffff) * 0.00390625f;
  float texY = float(texBits & 0xffff) * 0.00390625f;
  
  uint tileBits = floatBitsToUint(inTexCoord.y);
  float page = float(tileBits >> 24); // Texture array index, "page"
  float tileIndex = float(tileBits >> 12 & 0xfff); // Tile index
  float numTiles = float(tileBits & 0xfff); // Number of tiles per side

  texCoord = vec3(texX, texY, page); // Multiplication is a bit faster than division
  tile = vec2(tileIndex, numTiles); // Tile and how many tiles per side in page
}
