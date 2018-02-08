
// Texture array contains all textures in multiple atlases
uniform sampler2DArray m_ColorMap;

// Interpolated texture coordinates and an array index
in vec3 texCoord;

// Tile id and tile size packed together
flat in float tile;

void main() {
  vec4 color = vec4(1.0); // TODO support alpha channel

  // Extract data from tile
  uint bits = floatBitsToUint(tile);
  float numTiles = float(bits >> 16); // Tiles per side
  float tileId = float(bits & 0xffff); // Id of this tile

  vec3 tex = texCoord; // This is interpolated, not yet useful for us
  tex += vec3(mod(tile, numTiles), floor(tile / numTiles), 0); // Make it go to our tile
  tex /= numTiles; // Divide texture (yeah, weird) to make repeating work correctly

  color.rgb = texture(m_ColorMap, tex).rgb; // Now that we have the point, just draw

  gl_FragColor = color;
}
