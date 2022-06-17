@version 4.20
@include stdvert.ggsl

in vec2 texcoord;
in vec3 normal;
in vec4 color;
in vec3 position;

void main() {
    textureCoord = texcoord;
    pos = position;
    norm = normal;
	gl_Position = vec4(position, 1.0);
}
