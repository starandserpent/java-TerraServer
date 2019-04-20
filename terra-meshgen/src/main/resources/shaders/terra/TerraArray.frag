
uniform sampler2DArray m_ColorMap;

in vec3 texCoord;

void main() {
  vec4 color = vec4(1.0);// TODO support alpha channel

   color = texture(m_ColorMap, texCoord);

  gl_FragColor = color;
}
