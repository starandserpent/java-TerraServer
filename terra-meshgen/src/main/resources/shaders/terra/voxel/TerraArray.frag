uniform sampler2DArray m_ColorMap;

in vec3 normal;
in vec3 texCoord;

void main() {
    vec4 color = vec4(1.0);// TODO support alpha channel
    color = texture(m_ColorMap, texCoord);

    if (normal.y > 0){
        color.rgb += vec3(0.2, 0.2, 0.2);
        if(normal.z == 1 || normal.x == 0){
            color.rgb -= vec3(0.05, 0.05, 0.05);
        }
    }

    if(normal.z == 1 || normal.x == 0){
        color.rgb -= vec3(0.05, 0.05, 0.05);
    }

    gl_FragColor = color;
}
