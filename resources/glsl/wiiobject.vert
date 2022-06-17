@version 4.20
@include mapstruct.ggsl

in vec3 position;
in vec3 vs_normal;
in vec2 vs_uv0;
in vec2 vs_uv1;
in vec2 vs_uv2;
in vec2 vs_uv3;

in vec4 color;


layout(location = 0) out vec2 uv0;
layout(location = 1) out vec3 pos;
layout(location = 2) out vec3 normal;
layout(location = 3) out vec2 lightmapCoord;
layout(location = 4) out vec2 normalCoord;
layout(location = 5) out vec4 fs_layer0_color;

out gl_PerVertex{
    vec4 gl_Position;
    float gl_PointSize;
};

layout(binding = 0, set = 0) unwrap uniform VP{
    mat4 view;
    mat4 projection;
    vec3 camera;
};

layout(binding = 0, set = 1) unwrap uniform Model{
    mat4 model;
};

layout(set=7, binding=0) unwrap uniform HDRBlock{
    float exposure;
    float gamma;
};

uniform int invertY;
uniform vec3 billboardCenter;
uniform vec2 billboardSize;

void pipeColorSet0(){
    vec3 gammaMeshColor = pow(color.rgb, vec3(gamma));
    float meshAlpha = color.a;
    if(globalUseVertexColor == 0 || LAYER0_COLORSET == 0){
        gammaMeshColor = vec3(1);
    }
    if(globalUseMeshTransparency == 0 || LAYER0_COLORSET == 0){
        meshAlpha = 1;
    }

    fs_layer0_color = vec4(gammaMeshColor*8, meshAlpha*2);// should be * 2 but not implemented yet;
}

void main() {
    vec3 adjNorm = (vs_normal.xyz * 2) - 1;

    normal = normalize((model * vec4(adjNorm, 0.0)).xyz);

    if(vs_normal == vec3(0,0,0)){
        normal = vec3(0,0,0);
    }

    vec4 reverse = model * vec4(position, 1.0f);
    reverse.x = -reverse.x;
    pos = reverse.xyz;
    if(billboardSize.x != 0 && billboardSize.y != 0){
        vec3 camRight= vec3(view[0][0], view[1][0], view[2][0]);
        vec3 camUp = vec3(view[0][1], view[1][1], view[2][1]);
        pos = (billboardCenter*vec3(-1,1,1)) + camRight * pos.x * billboardSize.x + camUp * pos.y * billboardSize.y;
    }

    gl_Position = projection * view * vec4(pos, 1.0f);

    pipeColorSet0();

    uv0 = vs_uv0;
    if(LIGHTMAP_UVSET == 0){
        lightmapCoord = vs_uv0;
    }else if(LIGHTMAP_UVSET == 1){
        lightmapCoord = vs_uv1;
    }else if(LIGHTMAP_UVSET == 2){
        lightmapCoord = vs_uv2;
    }else{
        lightmapCoord = vs_uv3;
    }

    if(SURFACE_UVSET == 0){
        normalCoord = vs_uv0;
    }else if(SURFACE_UVSET == 1){
        normalCoord = vs_uv1;
    }else if(SURFACE_UVSET == 2){
        normalCoord = vs_uv2;
    }else if(SURFACE_UVSET == 3){
        normalCoord = vs_uv3;
    }

}