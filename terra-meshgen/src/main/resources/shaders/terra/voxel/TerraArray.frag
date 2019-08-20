uniform sampler2DArray m_ColorMap;

in vec3 normal;
in vec3 texCoord;

void main() {
    vec4 color = texture(m_ColorMap, vec3(texCoord.xy, round(texCoord.z)));

    if(color.a < 0.1)
        discard;

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
