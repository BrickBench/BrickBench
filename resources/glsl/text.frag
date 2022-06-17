@version 4.2
@include stdfrag.ggsl

float width = 0.5f;
float edge = 0.1f;

vec4 getTextTex(sampler2D tname){
        vec4 col = texture(tname, textureCoord);
       
        float dist = 1-col.a;
        float alpha = 1-smoothstep(width,width+edge,dist);
        vec3 colr = col.rgb;
        return vec4(colr, alpha);
}
void main() {
    fcolor = getTextTex(Kd);
	//fcolor = texture(Kd,textureCoord);
	//if(fcolor.a < 0.1f)
		//discard;
}
