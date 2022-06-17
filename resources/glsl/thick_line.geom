@version 4.2

layout(lines) in;
layout(triangle_strip, max_vertices=4) out;

in gl_PerVertex{
    vec4 gl_Position;
} gl_in[];

layout(location = 0) in vec2 textureCoord[];
layout(location = 1) in vec3 pos[];
layout(location = 2) in vec3 norm[];

out gl_PerVertex
{
    vec4 gl_Position;
    float gl_PointSize;
    float gl_ClipDistance[];
};

layout(location = 0) out vec4 FragPos;

layout(binding = 0, set = 0) unwrap uniform VP{
    mat4 view;
    mat4 projection;
    vec3 camera;
};

layout(binding = 0, set = 1) unwrap uniform Model{
    mat4 model;
};

void main()
{
    vec3 start = gl_in[0].gl_Position.xyz;
    vec3 end = gl_in[1].gl_Position.xyz;
    vec3 lhs = cross(normalize(end-start), vec3(0.0, 0.0, -0.1f));

    gl_Position = projection * view * model * vec4(start+lhs, 1.0);
    EmitVertex();
    gl_Position = projection * view * model * vec4(start-lhs, 1.0);
    EmitVertex();
    gl_Position = projection * view * model * vec4(end+lhs, 1.0);
    EmitVertex();
    gl_Position = projection * view * model * vec4(end-lhs, 1.0);
    EmitVertex();
    EndPrimitive();
}