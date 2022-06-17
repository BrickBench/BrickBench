@version 4.20
@include stdvert.ggsl

in vec3 position;
in vec3 normal;
in vec3 color;

layout(location = 3) out vec3 vcolor;

void main() {
    norm = normalize(vec3(model * vec4(normal,0.0)));
    pos = (model * vec4(position, 1.0f)).xyz;
    pos.x = -pos.x;
    gl_Position = projection * view * vec4(pos, 1.0f);
    gl_PointSize = 12f;

    vcolor = color;

}