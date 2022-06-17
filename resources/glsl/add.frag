@version 4.2
@include stdfrag.ggsl

uniform sampler2D Ka;

void main() {
	vec3 color1 = getTex(Kd).rgb;
	vec3 color2 = getTex(Ka).rgb;

    fcolor = vec4(color1.rgb + color2.rgb, 1);
}
