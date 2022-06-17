@version 4.20
@include stdvert.ggsl

in vec2 texcoord;
in vec3 normal;
in vec3 tangent;
in vec3 position;

layout(location = 4) out vec3 tan;

void main() {
    textureCoord = texcoord;
    norm = normalize(vec3(model * vec4(normal,0.0)));;
    tan = normalize(vec3(model * vec4(tangent,0.0)));
    pos = (model * vec4(position, 1.0f)).xyz;

    vec4 P = vec4(pos, 1.0f);
    vec4 V = view * P;
    gl_Position = projection * V;
}