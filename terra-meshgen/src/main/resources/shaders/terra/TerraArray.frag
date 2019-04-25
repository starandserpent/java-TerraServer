
uniform sampler2DArray m_ColorMap;

in vec3 texCoord;
in float postiony;

float y;

void main() {
    vec4 color = vec4(1.0);// TODO support alpha channel
    color = texture(m_ColorMap, texCoord);
    y = mod(postiony, 0.25);
    if (y > 0.001){
        color -= vec4(0.1, 0.1, 0.1, 0.0);
    }
    gl_FragColor = color;

}
