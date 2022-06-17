@version 4.5
@include stdfrag.ggsl

uniform vec2 direction;
uniform vec2 resolution;

void main()
{
    vec4 color = vec4(0.0);
    vec2 off1 = vec2(1.3846153846) * direction;
    vec2 off2 = vec2(3.2307692308) * direction;
    color += texture(Kd, textureCoord) * 0.2270270270;
    color += texture(Kd, textureCoord + (off1 / resolution)) * 0.3162162162;
    color += texture(Kd, textureCoord - (off1 / resolution)) * 0.3162162162;
    color += texture(Kd, textureCoord + (off2 / resolution)) * 0.0702702703;
    color += texture(Kd, textureCoord - (off2 / resolution)) * 0.0702702703;

    fcolor = color;
}