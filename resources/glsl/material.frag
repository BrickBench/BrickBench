@version 4.2
@include phong.ggsl
@include stdfrag.ggsl

uniform Material material;

void main() {
	generatePhongData();
	useMaterial(material);
	vec3 col = ambient;
	col += emmisive;

	for(int i = 0; i < numLights; i++){
		col += getPhongFrom(lights[i]);
	}

	fcolor = vec4(col, trans);
}
