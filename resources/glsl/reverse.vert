@version 4.20
@include stdvert.ggsl

in vec3 position;
in vec3 normal;
in vec2 texcoord;

void main() {
    textureCoord = texcoord;
    vec4 reverse = model * vec4(position, 1.0f);
    reverse.x = -reverse.x;
    norm = normalize(vec3(model * vec4(normal,0.0)));
    pos = reverse.xyz;
    gl_Position = projection * view * vec4(pos, 1.0f);
    gl_PointSize = 6f;
}