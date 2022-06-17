@version 4.2
layout (triangles) in;
layout (triangle_strip, max_vertices = 3) out;

in gl_PerVertex{
  vec4 gl_Position;
  float gl_PointSize;
} gl_in[];

out gl_PerVertex
{
    vec4 gl_Position;
};

layout(location = 2) in vec3 norm[];
layout(location = 3) in vec3 vcolor[];

out vec3 normal;
out vec3 bary;
out vec3 fragcolor;

uniform mat4 projection;

void main(){
    gl_Position = gl_in[0].gl_Position;
    normal = norm[0];
    bary = vec3(0,0,1);
    fragcolor = vcolor[0];
    EmitVertex();

    gl_Position = gl_in[1].gl_Position;
    normal = norm[1];
    bary = vec3(0,1,0);
    fragcolor = vcolor[1];
    EmitVertex();

    gl_Position = gl_in[2].gl_Position;
    normal = norm[2];
    bary = vec3(1,0,0);
    fragcolor = vcolor[2];
    EmitVertex();
    EndPrimitive();
}