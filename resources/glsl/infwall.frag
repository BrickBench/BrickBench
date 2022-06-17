@version 4.2

uniform layout(set = 1, binding = 1) sampler2D Kd;

in vec2 uvcoord;

layout(location = 0) out vec4 fcolor;

void main() {
    vec4 color = texture(Kd, uvcoord);

    fcolor = vec4(color.rgb,1);
    if(color.a < 0.5f) discard;
}
