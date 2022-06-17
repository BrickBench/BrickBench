@version 4.2
@include stdfrag.ggsl

uniform int text;

vec4 getScreenTex(sampler2D tname){
    if(text == 1){
        vec4 col = texture(tname, textureCoord);
        float width = 0.4f;
        float edge = 0.2f;
        float dist = 1-col.a;
        float alpha = 1-smoothstep(width,width+edge,dist);
        vec3 colr = col.rgb;
        return vec4(colr, alpha);
    }
	
    return texture(tname, textureCoord);
}

void main() {
    fcolor = getScreenTex(Kd);
	if(fcolor.a < 0.1f)
		discard;
}
