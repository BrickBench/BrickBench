@version 4.2
@include mapstruct.ggsl

layout(location = 0) out vec4 fcolor;

layout(location = 0) in vec2 uv0;
layout(location = 1) in vec3 pos;
layout(location = 2) in vec3 normal;
layout(location = 3) in vec2 lightmapCoord;
layout(location = 4) in vec2 normalCoord;
layout(location = 5) in vec4 fs_layer0_color;

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
float specularFactor = 0;
float ldotn0;
float ldotn1;
float ldotn2;

vec3 getLightColor(){
    vec3 diffuseLight = vec3(0);
    vec3 specularLight = vec3(0);

    if((globalUseLightmaps == 1 && LIGHTMAP_STAGE != 0 && lightmapReady == 1)){
      //  if((LIGHTMAP_STAGE == 2 && false) || globalForceLightmapUsage == 1){
        if(lightmapReady == 0 && LIGHTMAP_STAGE == 0){
            vec3 weights = vec3(
            dot(surfaceNormal.rgb, vec3(-0.4082482904,	-0.7071067811,	0.5773502691)),
            dot(surfaceNormal.rgb, vec3(-0.4082482904,	0.7071067811,	0.5773502691)),
            dot(surfaceNormal.rgb, vec3(0.8164965809,	0.0,			0.5773502691))
            );
            weights = max(weights, vec3(0));

            vec4 lmcol1 = texture(lightmap1, lightmapCoord);
            vec4 lmcol2 = texture(lightmap3, lightmapCoord);
            vec4 lmcol3 = texture(lightmap4, lightmapCoord);
            diffuseLight = (lmcol1.rgb * weights.x) + (lmcol2.rgb * weights.y) + (lmcol3.rgb * weights.z)  * vec3(8);

            diffuseLight = max(diffuseLight, vec3(0.2f)); //temporary
        }else{
            diffuseLight = texture(lightmap1, lightmapCoord).rgb;
        }

        if(lightmapCoord.x < 0.0f || lightmapCoord.x == 0.0f){
            diffuseLight = vec3(1);
            if(PRELIGHT_FX == 1 && PRELIGHT_FX_LIVE_SPECULAR == 1){
                specularFactor = 0.3 * fs_layer0_color.b + 0.59 * fs_layer0_color.g + 0.11 * fs_layer0_color.r;
            }
        }else{
            if(PRELIGHT_FX == 1 && PRELIGHT_FX_LIVE_SPECULAR == 1){
                specularFactor = 0.3 * diffuseLight.r + 0.59 * diffuseLight.g + 0.11 * diffuseLight.b;
            }
        }
    }else if (globalUseDynamicLights == 1 && LIGHTING_STAGE != 0){
        if(PRELIGHT_FX == 1){
            diffuseLight = vec3(1);
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

            diffuseLight = max(vec3(0.2f), diffuseLight);
        }
    }else{
        diffuseLight = vec3(1);
        specularLight = vec3(1);
    }

    return diffuseLight;
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

vec4 shadeSurface(vec4 surfaceColor, vec3 diffuseLight){
    if(LIGHTMAP_STAGE != 0 && globalUseLightmaps == 1){
        if(PRELIGHT_FX == 1 && LIGHTMAP_STAGE == 2){
            return vec4(surfaceColor.rgb * diffuseLight.rgb, surfaceColor.a);
        }else{
            return vec4(surfaceColor.rgb * diffuseLight.rgb, surfaceColor.a);
        }
    }else if(PRELIGHT_FX == 1){
        return vec4(surfaceColor.rgb * fs_layer0_color.bgr, surfaceColor.a);
    }else if(LIGHTING_STAGE == 0){
        return surfaceColor;
    }else{
        return vec4(surfaceColor.rgb * diffuseLight.rgb, surfaceColor.a);
    }
}

void calculateNormal(){
    if(SURFACE_TYPE == 1 && false){
        surfaceNormal = ((texture(surface_sampler, normalCoord).agbr * 2) - 1) * vec4(normal,1);
    }else{
        surfaceNormal = vec4(normal,1);
    }

    if(surfaceNormal.rgb == vec3(0,0,0)){
        surfaceNormal = vec4(0,1,0,1);
    }

    surfaceNormal.xyz = normalize(surfaceNormal.xyz);
}

void compute_ldotn(){
    if(LIGHTING_LIGHTS_COUNT > 0){
        ldotn0 = max(0, dot(surfaceNormal.rgb, normalize(light0.pos - pos)));
    }
    if(LIGHTING_LIGHTS_COUNT > 1){
        ldotn1 = max(0, dot(surfaceNormal.rgb, normalize(light1.pos - pos)));
    }
    if(LIGHTING_LIGHTS_COUNT > 2){
        ldotn2 = max(0, dot(surfaceNormal.rgb, normalize(light2.pos - pos)));
    }
}

void main() {
    calculateNormal();
    compute_ldotn();

    vec4 surfaceColor = getColor();
    vec3 light = vec3(1);
    if(globalApplyLights == 1){
        light = getLightColor();
    }
    vec4 shadedSurface = shadeSurface(surfaceColor, light);

    fcolor = shadedSurface;

    if(muteColors != 0){
        float luminance = dot(fcolor.rgb, vec3(0.2125, 0.7154, 0.0721));
        fcolor.rgb = vec3(luminance, luminance, luminance) * 0.4f;
        fcolor.a = min(fcolor.a, 0.4f);
    }

    if(fcolor.a < alphaCutoff) discard;
}
