@version 4.2
@include stdfrag.ggsl

void main() {
    fcolor = texture(Kd, textureCoord) * vec4(1,0,0,1);
    //if(fcolor.a < 0.1f) discard;
}