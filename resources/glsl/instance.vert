@version 420
@include stdvert.ggsl

in vec2 texcoord;
in vec3 normal;
in vec3 offset;
in vec3 position;

void main() {
    textureCoord = texcoord;
    norm = normalize(vec3(model * vec4(normal,0.0)));
	pos = offset + position;
    vec4 P = view * vec4(pos.xyz, 1);
    gl_Position = projection * P;
}