@version 4.5
@include stdfrag.ggsl

void main()
{
    vec3 luminanceVector = vec3(0.2126, 0.7152, 0.0722);
    vec4 color = texture(Kd, textureCoord);
    float luminance = dot(color.rgb, luminanceVector);
    luminance = max(0.0, luminance - 0.7f);
    color.xyz *= sign(luminance);
    color.a = 1.0;

    fcolor = color;
}