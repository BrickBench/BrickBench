COMPILED GGSL ERROR SOURCE: From shader wiiobject.frag with error code 0: ERROR: 0:170: 'meshMute' : undeclared identifier
#version 420
#extension GL_ARB_explicit_uniform_location : require

uniform int SURFACE_TYPE;
uniform int LIGHTMAP_UVSET;
uniform int SURFACE_UVSET;
uniform int COMBINE_OP_0;
uniform int PRELIGHT_FX_LIVE_SPECULAR;
uniform int PRELIGHT_FX;
uniform int LAYER0_COLORSET;
uniform int LIGHTING_STAGE;
uniform int LIGHTMAP_STAGE;
uniform vec4 layer0_diffuse;
uniform float alphaCutoff;
uniform int lightmapReady;
uniform int meshEditTransparency;
uniform int globalUseVertexColor;
uniform int globalUseMeshTransparency;
uniform int globalUseDynamicLights;
uniform int globalUseLightmaps;
uniform int globalApplyLights;
struct LegoLight {
    vec3 pos;
    vec3 color;
} ;
uniform int LIGHTING_LIGHTS_COUNT;
uniform LegoLight light0;
uniform LegoLight light1;
uniform LegoLight light2;
uniform layout(binding = 8) sampler2D layer0_sampler;
uniform layout(binding = 9) sampler2D surface_sampler;
uniform layout(binding = 10) sampler2D lightmap1;
uniform layout(binding = 11) sampler2D lightmap2;
uniform layout(binding = 12) sampler2D lightmap3;
uniform layout(binding = 13) sampler2D lightmap4;
uniform mat4 model;
uniform vec3 camera;
uniform mat4 perspective;
uniform mat4 view;
uniform float gamma;
uniform float exposure;
layout(location = 0) out vec4 fcolor;
layout(location = 0) in vec2 uv0;
layout(location = 1) in vec3 pos;
layout(location = 2) in vec3 normal;
layout(location = 3) in vec2 lightmapCoord;
layout(location = 4) in vec2 normalCoord;
layout(location = 5) in vec4 fs_layer0_color;
uniform layout(binding = 1) sampler2D Kd;
vec4 surfaceNormal;
float specularFactor = 0;
float ldotn0;
float ldotn1;
float ldotn2;
vec3 getLightColor() {
    vec3 diffuseLight = vec3(0);
    vec3 specularLight = vec3(0);
    if((((globalUseLightmaps == 1) && (LIGHTMAP_STAGE != 0)) && (lightmapReady == 1))){
        if(((lightmapReady == 0) && (LIGHTMAP_STAGE == 0))){
            vec3 weights = vec3(dot(surfaceNormal.rgb, vec3(-0.40824828f, -0.70710677f, 0.57735026f)), dot(surfaceNormal.rgb, vec3(-0.40824828f, 0.70710677f, 0.57735026f)), dot(surfaceNormal.rgb, vec3(0.8164966f, 0.0f, 0.57735026f)));
            weights = max(weights, vec3(0));
            vec4 lmcol1 = texture(lightmap1, lightmapCoord);
            vec4 lmcol2 = texture(lightmap3, lightmapCoord);
            vec4 lmcol3 = texture(lightmap4, lightmapCoord);
            diffuseLight = (((lmcol1.rgb * weights.x) + (lmcol2.rgb * weights.y)) + ((lmcol3.rgb * weights.z) * vec3(8)));
            diffuseLight = max(diffuseLight, vec3(0.2f));
        }else{
            diffuseLight = texture(lightmap1, lightmapCoord).rgb;
        };
        if(((lightmapCoord.x < 0.0f) || (lightmapCoord.x == 0.0f))){
            diffuseLight = vec3(1);
            if(((PRELIGHT_FX == 1) && (PRELIGHT_FX_LIVE_SPECULAR == 1))){
                specularFactor = (((0.3f * fs_layer0_color.b) + (0.59f * fs_layer0_color.g)) + (0.11f * fs_layer0_color.r));
            };
        }else{
            if(((PRELIGHT_FX == 1) && (PRELIGHT_FX_LIVE_SPECULAR == 1))){
                specularFactor = (((0.3f * diffuseLight.r) + (0.59f * diffuseLight.g)) + (0.11f * diffuseLight.b));
            };
        };
    }else{
        if(((globalUseDynamicLights == 1) && (LIGHTING_STAGE != 0))){
            if((PRELIGHT_FX == 1)){
                diffuseLight = vec3(1);
            }else{
                if((LIGHTING_LIGHTS_COUNT > 0)){
                    diffuseLight += (ldotn0 * light0.color);
                };
                if((LIGHTING_LIGHTS_COUNT > 1)){
                    diffuseLight += (ldotn1 * light1.color);
                };
                if((LIGHTING_LIGHTS_COUNT > 2)){
                    diffuseLight += (ldotn2 * light2.color);
                };
                diffuseLight = max(vec3(0.2f), diffuseLight);
            };
        }else{
            diffuseLight = vec3(1);
            specularLight = vec3(1);
        };
    };
    return diffuseLight;
} ;
vec4 getColor() {
    vec4 baseColor = vec4(1);
    if(((PRELIGHT_FX == 1) && (LIGHTMAP_STAGE == 0))){
        baseColor = vec4(1, 1, 1, fs_layer0_color.a);
    }else{
        baseColor = fs_layer0_color.bgra;
    };
    vec4 color = vec4(1);
    if((COMBINE_OP_0 == 0)){
        color = (baseColor * vec4(pow(layer0_diffuse.rgb, vec3(gamma)), 1));
    }else{
        vec4 samplerColor = texture(layer0_sampler, uv0);
        color = (samplerColor * baseColor);
    };
    return color;
} ;
vec4 shadeSurface(vec4 surfaceColor, vec3 diffuseLight) {
    if(((LIGHTMAP_STAGE != 0) && (globalUseLightmaps == 1))){
        if(((PRELIGHT_FX == 1) && (LIGHTMAP_STAGE == 2))){
            return vec4((surfaceColor.rgb * diffuseLight.rgb), surfaceColor.a);
        }else{
            return vec4((surfaceColor.rgb * diffuseLight.rgb), surfaceColor.a);
        };
    }else{
        if((PRELIGHT_FX == 1)){
            return vec4((surfaceColor.rgb * fs_layer0_color.bgr), surfaceColor.a);
        }else{
            if((LIGHTING_STAGE == 0)){
                return surfaceColor;
            }else{
                return vec4((surfaceColor.rgb * diffuseLight.rgb), surfaceColor.a);
            };
        };
    };
} ;
void calculateNormal() {
    if(((SURFACE_TYPE == 1) && false)){
        surfaceNormal = (((texture(surface_sampler, normalCoord).agbr * 2) - 1) * vec4(normal, 1));
    }else{
        surfaceNormal = vec4(normal, 1);
    };
    if((surfaceNormal.rgb == vec3(0, 0, 0))){
        surfaceNormal = vec4(0, 1, 0, 1);
    };
    surfaceNormal.xyz = normalize(surfaceNormal.xyz);
} ;
void compute_ldotn() {
    if((LIGHTING_LIGHTS_COUNT > 0)){
        ldotn0 = max(0, dot(surfaceNormal.rgb, normalize((light0.pos - pos))));
    };
    if((LIGHTING_LIGHTS_COUNT > 1)){
        ldotn1 = max(0, dot(surfaceNormal.rgb, normalize((light1.pos - pos))));
    };
    if((LIGHTING_LIGHTS_COUNT > 2)){
        ldotn2 = max(0, dot(surfaceNormal.rgb, normalize((light2.pos - pos))));
    };
} ;
void main() {
    calculateNormal();
    compute_ldotn();
    vec4 surfaceColor = getColor();
    vec3 light = vec3(1);
    if((globalApplyLights == 1)){
        light = getLightColor();
    };
    vec4 shadedSurface = shadeSurface(surfaceColor, light);
    fcolor = shadedSurface;
    if((meshMute == 1)){
        fcolor.rgb = (fcolor.rgb * 0.2f);
        fcolor.a = min(fcolor.a, 0.4f);
    };
    if((fcolor.a < alphaCutoff)){
        discard;
    };
} ;
;
