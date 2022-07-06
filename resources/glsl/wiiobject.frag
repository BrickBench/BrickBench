@version 4.2
@include mapstruct.ggsl

layout(location = 0) out vec4 fcolor;

layout(location = 0) in vec2 uv0;
layout(location = 1) in vec3 pos;
layout(location = 2) in vec3 varying_normal;
layout(location = 3) in vec2 lightmapCoord;
layout(location = 4) in vec2 normalCoord;
layout(location = 5) in vec2 specularCoord;
layout(location = 6) in vec4 fs_layer0_color;
layout(location = 7) in vec4 varying_lightDirSet;
layout(location = 8) in vec3 varying_tangent;
layout(location = 9) in vec3 varying_bitangent;

layout(set=7, binding=0) unwrap uniform HDRBlock{
    float exposure;
    float gamma;
};

layout(binding = 0, set = 0) unwrap uniform VP{
    mat4 view;
    mat4 perspective;
    vec3 camera;
};

layout(binding = 0, set = 1) unwrap uniform Model{
    mat4 model;
};

uniform layout(set = 1, binding = 1) sampler2D Kd;

vec4 surfaceNormal;
vec3 normal;
float fs_prelitSpecularFactor = 0;
float ldotn0 = 0;
float ldotn1 = 0;
float ldotn2 = 0;
vec3 diffuseLight = vec3(0);
vec3 specularLight = vec3(0);
vec3 incident;

vec3 phongSpecularStage () {
    vec3 specPhong = vec3(0);

    if (PRELIGHT_FX == 1 && PRELIGHT_FX_LIVE_SPECULAR != 1) {
        specPhong += fs_layer0_color.bgr * pow(max(0, dot(incident, reflect(-normalize(varying_lightDirSet.rgb), normal.rgb))), specular_params.r);
        return specPhong * specular_params.g;
    } else {
        if(LIGHTING_LIGHTS_COUNT > 0){
            specPhong += light0.color * pow(max(0, dot(incident, reflect(-normalize(light0.pos), normal.rgb))), specular_params.r);
        }
        if(LIGHTING_LIGHTS_COUNT > 1){
            specPhong += light1.color * pow(max(0, dot(incident, reflect(-normalize(light1.pos), normal.rgb))), specular_params.r);
        }
        if(LIGHTING_LIGHTS_COUNT > 2){
            specPhong += light2.color * pow(max(0, dot(incident, reflect(-normalize(light2.pos), normal.rgb))), specular_params.r);
        }

        if (PRELIGHT_FX == 1 && PRELIGHT_FX_LIVE_SPECULAR == 1) {
            specPhong *= fs_prelitSpecularFactor;
        }

        return specPhong * specular_params.g;
    }
}

void getLightColor(){
    if (LIGHTMAP_STAGE == 0 || lightmapReady == 0) {
        if (LIGHTING_STAGE == 0) {
            diffuseLight = vec3(1,1,1);
        }

        if(PRELIGHT_FX == 1 && PRELIGHT_FX_LIVE_SPECULAR == 1){
            fs_prelitSpecularFactor = 0.3 * fs_layer0_color.b + 0.59 * fs_layer0_color.g + 0.11 * fs_layer0_color.r;
        }
    } else if (LIGHTMAP_STAGE == 1 || LIGHTMAP_STAGE == 2) {
        diffuseLight = texture(lightmap1, lightmapCoord).rgb;

        if(PRELIGHT_FX == 1 && PRELIGHT_FX_LIVE_SPECULAR == 1){
            fs_prelitSpecularFactor = 0.3 * diffuseLight.r + 0.59 * diffuseLight.g + 0.11 * diffuseLight.b;
        }
    } else if(LIGHTMAP_STAGE == 2 && false){
        vec3 weights = vec3(
        max(dot(surfaceNormal.rgb, vec3(-0.4082482904,	-0.7071067811,	0.5773502691)), 0),
        max(dot(surfaceNormal.rgb, vec3(-0.4082482904,	0.7071067811,	0.5773502691)), 0),
        max(dot(surfaceNormal.rgb, vec3(0.8164965809,	0.0,			0.5773502691)), 0));

        vec4 lmcol1 = texture(lightmap2, lightmapCoord);
        vec3 lmcol2 = texture(lightmap3, lightmapCoord).rgb;
        vec3 lmcol3 = texture(lightmap4, lightmapCoord).rgb;
        diffuseLight = (lmcol1.rgb * weights.x) + (lmcol2.rgb * weights.y) + (lmcol3.rgb * weights.z);
        if(PRELIGHT_FX == 1 && PRELIGHT_FX_LIVE_SPECULAR == 1){
            fs_prelitSpecularFactor = 0.3 * diffuseLight.r + 0.59 * diffuseLight.g + 0.11 * diffuseLight.b;
        }

        if(lightmapCoord.x < 0.0f || lightmapCoord.x == 0.0f){
            diffuseLight = vec3(1);   
            if(PRELIGHT_FX == 1 && PRELIGHT_FX_LIVE_SPECULAR == 1){
                fs_prelitSpecularFactor = 0.3 * fs_layer0_color.b + 0.59 * fs_layer0_color.g + 0.11 * fs_layer0_color.r;
            }
        }
    }

    if (LIGHTING_STAGE != 0){
        if(PRELIGHT_FX == 1){
            if(LIGHTMAP_STAGE == 0) {
                diffuseLight = vec3(1);
            }
        }else{
            if(LIGHTING_LIGHTS_COUNT > 0){
                diffuseLight += ldotn0 * light0.color;
            }
            if(LIGHTING_LIGHTS_COUNT > 1){
                diffuseLight += ldotn1 * light1.color;
            }
            if(LIGHTING_LIGHTS_COUNT > 2){
                diffuseLight += ldotn2 * light2.color;
            }

            if (LIGHTING_STAGE != 1) {
                diffuseLight = diffuseLight + ambientColor.rgb;
            }
        }
        if (LIGHTING_STAGE == 5) {
            specularLight = phongSpecularStage();
        }
    }
}

vec4 getColor(){
    vec4 baseColor = vec4(1);

    if(PRELIGHT_FX == 1 && LIGHTMAP_STAGE == 0){
        baseColor = vec4(1,1,1,fs_layer0_color.a);
    }else{
        baseColor = fs_layer0_color.bgra;
    }
    vec4 surfaceColor = vec4(1);

    //THIS CASE SHOULD BE IMPOSSIBLE BASED OFF OF THE COMBINE_OP_0 CODE IN SHADERBUILDERGEN/FILEMATERIAL

    //if (COMBINE_OP_0 == 0){
   //     surfaceColor = baseColor * vec4(pow(layer0_diffuse.rgb, vec3(gamma)), 1);
   // } else {
        if(LAYER0_DIFFUSEENABLE == 1){
            if(LIGHTING_STAGE == 0){
                surfaceColor = baseColor;
            }else{
                surfaceColor = baseColor * vec4(pow(layer0_diffuse.rgb, vec3(gamma)), 1);
            }
        }else{
            vec4 samplerColor = texture(layer0_sampler, uv0);
            surfaceColor = samplerColor * baseColor;
        }
   // }

    return surfaceColor;
}

vec4 shadeSurface(vec4 surfaceColor, vec3 reflectivity){
    if(LIGHTMAP_STAGE != 0 && globalUseLightmaps == 1){
        if(PRELIGHT_FX == 1 && LIGHTMAP_STAGE == 1){
            float bump = dot(varying_lightDirSet.xyz, normal.xyz);
            float plain = dot(varying_lightDirSet.xyz, normalize(varying_normal.xyz));
            return vec4(surfaceColor.rgb * diffuseLight.rgb * (1 + (bump - plain)) + reflectivity, surfaceColor.a);
        }else{
            return vec4(surfaceColor.rgb * diffuseLight.rgb + reflectivity, surfaceColor.a);
        }
    }else if(PRELIGHT_FX == 1){
        float bump = dot(varying_lightDirSet.xyz, normal.xyz);
        float plain = dot(varying_lightDirSet.xyz, normalize(varying_normal.xyz));
        return vec4(surfaceColor.rgb * fs_layer0_color.bgr * (1 + (bump - plain)) + reflectivity, surfaceColor.a);
    }else if(LIGHTING_STAGE == 0){
        return surfaceColor;
    }else{
        return vec4(surfaceColor.rgb * diffuseLight.rgb + reflectivity, surfaceColor.a);
    }
}

float fresnelStage() {
    return mix(specular_params.b, 1, pow(1 - dot(normal, incident), specular_params.a));
}

vec4 reflectivityStage() {
    if (SPECULAR_SPECULARENABLE == 1) {
        return specular_specular;
    } else {
        return texture(specular_sampler, specularCoord);
    }
}

void computeSurfaceNormal(){
    if(SURFACE_TYPE == 1){
        surfaceNormal = ((texture(surface_sampler, normalCoord).agbr * 2) - 1);
    }else{
        surfaceNormal = vec4(normal,1);
    }

    if(surfaceNormal.rgb == vec3(0,0,0)){
        surfaceNormal = vec4(0,1,0,1);
    }

    surfaceNormal.xyz = normalize(surfaceNormal.xyz);
}

void computeNormal() {
    if (SURFACE_TYPE == 1) {
        normal = normalize(surfaceNormal.x * normalize(varying_tangent.xyz) + surfaceNormal.y * normalize(varying_bitangent.xyz) + surfaceNormal.z * varying_normal.xyz);
    } else {
        normal = normalize(varying_normal);
    }
}

void compute_ldotn(){
    if(LIGHTING_LIGHTS_COUNT > 0){
        ldotn0 = max(0, dot(normalize(light0.pos), normal.rgb));
    }
    if(LIGHTING_LIGHTS_COUNT > 1){
        ldotn1 = max(0, dot(normalize(light1.pos), normal.rgb));
    }
    if(LIGHTING_LIGHTS_COUNT > 2){
        ldotn2 = max(0, dot(normalize(light2.pos), normal.rgb));
    }
}

void main() {
    computeSurfaceNormal();
    computeNormal();
    compute_ldotn();

    incident = normalize(camera * vec3(-1,1,1) - pos); 
    
    vec4 surfaceColor = getColor();
    getLightColor();

    vec4 reflectivity = vec4(1);
    float fresnel = 1;

    if (FRESNEL_STAGE == 1) {
        fresnel = fresnelStage();
    }

    if (REFLECTIVITY_STAGE == 1) {
        reflectivity = reflectivityStage() * fresnel;
    }

    vec4 shadedSurface = shadeSurface(surfaceColor, specularLight * reflectivity.rgb);

    fcolor = shadedSurface;
    if(muteColors != 0){
        float luminance = dot(fcolor.rgb, vec3(0.2125, 0.7154, 0.0721));
        fcolor.rgb = vec3(luminance, luminance, luminance) * 0.4f;
        fcolor.a = min(fcolor.a, 0.4f);
    }

    if(fcolor.a < alphaCutoff) discard;
}
