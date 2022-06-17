@version 4.2
layout (lines) in;
layout (triangle_strip, max_vertices = 4) out;

in gl_PerVertex{
    vec4 gl_Position;
    float gl_PointSize;
} gl_in[];

out gl_PerVertex
{
    vec4 gl_Position;
    float gl_PointSize;
    float gl_ClipDistance[];
};

layout(binding = 0, set = 0) unwrap uniform VP{
    mat4 view;
    mat4 projection;
    vec3 camera;
};

layout(binding = 0, set = 1) unwrap uniform Model{
    mat4 model;
};

layout(location = 0) in vec2 textureCoord[];
layout(location = 1) in vec3 pos[];
layout(location = 2) in vec3 norm[];


out vec2 uvcoord;

float SCALE = 1000;

void main(){

    uvcoord = vec2(textureCoord[0].x, -SCALE);
    gl_Position = projection * view * model * vec4(-pos[0].x, -SCALE, pos[0].z, 1.0f);
    EmitVertex();

    uvcoord = vec2(textureCoord[0].x, SCALE);
    gl_Position = projection * view * model * vec4(-pos[0].x, SCALE, pos[0].z, 1.0f);
    EmitVertex();

    uvcoord = vec2(textureCoord[1].x, -SCALE);
    gl_Position = projection * view * model * vec4(-pos[1].x, -SCALE, pos[1].z, 1.0f);
    EmitVertex();

    uvcoord = vec2(textureCoord[1].x, SCALE);
    gl_Position = projection * view * model * vec4(-pos[1].x, SCALE, pos[1].z, 1.0f);
    EmitVertex();

    EndPrimitive();
}
