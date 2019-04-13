
// Texture array contains all textures in multiple atlases
uniform sampler2DArray m_ColorMap;

// Interpolated texture coordinates and page index
in vec3 texCoord;

// Tile id and tile size
flat in vec2 tile;

void main() {
  vec4 color = vec4(1.0); // TODO support alpha channel

  vec2 tex = vec2(mod(texCoord.x, 1.0f), mod(texCoord.y, 1.0f)); // We repeat, so take only decimal part
  tex += vec2(mod(tile.x, tile.y), floor(tile.x / tile.y)); // Make it go to our tile
  tex /= tile.y; // Divide texture coordinates by number of tiles so we only end inside our tile

  color.rgb = texture(m_ColorMap, vec3(tex, texCoord.z)).rgb; // Draw for correct texture array page (Z)

  gl_FragColor = color;
}
