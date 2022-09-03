@version 4.20
@include mapstruct.ggsl

in vec3 position;
in vec4 vs_normal;
in vec4 tangent;
in vec4 bitangent;
in vec2 vs_uv0;
in vec2 vs_uv1;
in vec2 vs_uv2;
in vec2 vs_uv3;
in vec4 lightDir;
in vec4 color;


layout(location = 0) out vec2 uv0;
layout(location = 1) out vec3 pos;
layout(location = 2) out vec3 varying_normal;
layout(location = 3) out vec2 lightmapCoord;
layout(location = 4) out vec2 normalCoord;
layout(location = 5) out vec2 specularCoord;
layout(location = 6) out vec4 fs_layer0_color;
layout(location = 7) out vec4 varying_lightDirSet;
layout(location = 8) out vec3 varying_tangent;
layout(location = 9) out vec3 varying_bitangent;

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
uniform vec4 lightmapOffset;

void pipeColorSet0(){
    vec3 gammaMeshColor = pow(color.rgb, vec3(gamma));
    float meshAlpha = color.a;
    if(LAYER0_COLORSET == 0){
        gammaMeshColor = vec3(1);
        meshAlpha = 1;
    }

    fs_layer0_color = vec4(gammaMeshColor*8, meshAlpha*2);// should be * 2 but not implemented yet;
}

void pipeLightDirSet() {
   varying_lightDirSet = view * model * lightDir;
}

void main() {
    varying_normal = normalize(model * vec4((vs_normal.xyz * 2) - 1, 0)).xyz;
    varying_tangent = normalize(model * vec4((tangent.xyz * 2) - 1, 0)).xyz;
    if (bitangent == vec4(0)) {
        varying_bitangent = normalize(cross(varying_tangent.xyz, varying_normal.xyz));
    } else {
        varying_bitangent = normalize(model * vec4((bitangent.xyz * 2) - 1, 0)).xyz;
    }
    
    pos = (model * vec4(position, 1.0f)).xyz;
    
    vec3 reverse = vec3(-pos.x, pos.y, pos.z);
    if(billboardSize.x != 0 && billboardSize.y != 0){
        vec3 camRight= vec3(view[0][0], view[1][0], view[2][0]);
        vec3 camUp = vec3(view[0][1], view[1][1], view[2][1]);
        reverse = billboardCenter * vec3(-1,1,1) + camRight * reverse.x * billboardSize.x + camUp * reverse.y * billboardSize.y;
        pos = reverse;
        pos.x = -pos.x;
    }

    gl_Position = projection * view * vec4(reverse, 1.0f);

    pipeColorSet0();
    pipeLightDirSet();

    uv0 = vs_uv0;
    if(LIGHTMAP_UVSET == 0){
        lightmapCoord = vs_uv0 * lightmapOffset.zw + lightmapOffset.xy;
    }else if(LIGHTMAP_UVSET == 1){
        lightmapCoord = vs_uv1 * lightmapOffset.zw + lightmapOffset.xy;
        //lightmapCoord = lightmapOffset.xy;
    }else if(LIGHTMAP_UVSET == 2){
        lightmapCoord = vs_uv2 * lightmapOffset.zw + lightmapOffset.xy;
    }else{
        lightmapCoord = vs_uv3 * lightmapOffset.zw + lightmapOffset.xy;
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
    
    if(SPECULAR_UVSET == 0){
        specularCoord = vs_uv0;
    }else if(SPECULAR_UVSET == 1){
        specularCoord = vs_uv1;
    }else if(SPECULAR_UVSET == 2){
        specularCoord = vs_uv2;
    }else if(SPECULAR_UVSET == 3){
        specularCoord = vs_uv3;
    }
}
