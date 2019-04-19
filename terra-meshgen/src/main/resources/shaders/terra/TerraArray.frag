
uniform sampler2D m_ColorMap;

in vec2 texCoord;

void main() {
  vec4 color = vec4(1.0);// TODO support alpha channel

  color = texture2D(m_ColorMap, vec2(texCoord.x-TILE*floor(texCoord.x/TILE), texCoord.y - TILE*floor(texCoord.y/TILE)));

  gl_FragColor = color;
}
