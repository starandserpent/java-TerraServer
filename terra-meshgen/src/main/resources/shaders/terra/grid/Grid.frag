in vec2 texCoord;
in float dist;

void main() {
    vec4 color = vec4(1.0);
    float grid = clamp(step(dist,fract(texCoord.x * float(100))) * step(dist,fract(texCoord.y * float(100))),0.4,1.0);
    gl_FragColor = color * grid;
}
