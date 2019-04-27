uniform sampler2DArray m_ColorMap;

in vec3 normal;
in vec3 texCoord;
out vec3 pos;

void main() {
    vec4 color = vec4(1.0);// TODO support alpha channel
    color = texture(m_ColorMap, texCoord);
    float y = mod(pos.y * 100, 2.5);
    gl_FragColor = color;
    if (normal.y > 0){
        color.rgb -= vec3(0.1, 0.1, 0.1);
    }

    gl_FragColor = color;

}
