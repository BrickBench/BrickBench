@version 4.2
@include stdfrag.ggsl

void main() {
    fcolor = texture(Kd, textureCoord);
    if(fcolor.a < 0.01f) discard;
}