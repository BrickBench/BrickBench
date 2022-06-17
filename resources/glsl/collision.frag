layout(location = 0) out vec4 fcolor;

layout(binding = 0, set = 0) unwrap uniform VP{
    mat4 view;
    mat4 perspective;
    vec3 camera;
};

layout(set=7, binding=0) unwrap uniform HDRBlock{
    float exposure;
    float gamma;
};

in vec3 normal;
in vec3 bary;
in vec3 fragcolor;

uniform int muteColors;

void main() {

    float minBary = min(bary.x, min(bary.y, bary.z));
    float delta = abs(dFdx(minBary)) + abs(dFdy(minBary));
	minBary = smoothstep(0, delta, minBary);
	vec3 normalColor = ((normal) * 0.5)+vec3(0.5);

    vec3 color = vec3(0,0,0);

    if(fragcolor == vec3(0) || muteColors != 0){
        color = normalize(normalColor) * 0.3f;
    }else{
        color = pow(fragcolor, vec3(gamma));
    }

    fcolor = vec4(color*minBary,1);
    if(muteColors != 0){
        float luminance = dot(fcolor.rgb, vec3(0.2125, 0.7154, 0.0721));
        fcolor.rgb = vec3(luminance, luminance, luminance) * 0.4f;
        fcolor.a = min(fcolor.a, 0.4f);
    }
}