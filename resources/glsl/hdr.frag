@version 4.2
@include stdfrag.ggsl

layout(set=7, binding=0) unwrap uniform HDRBlock{
    float exposure;
    float gamma;
};

void main() {
    vec3 color = texture(Kd, textureCoord).rgb;

    color = pow(color, vec3(1.0f / gamma));
  
    fcolor = vec4(color, 1.0f);
}
