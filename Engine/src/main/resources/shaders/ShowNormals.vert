#version 330 core

layout (location = 0) in vec3 a_Pos;
layout (location = 1) in vec3 a_Normal;
layout (location = 3) in vec3 a_Tangent;
layout (location = 4) in vec3 a_BiTangent;

out Data {
    vec3 normal;
    vec3 tangent;
    vec3 biTangent;
} vert_out;

void main()
{
    vert_out.normal = a_Normal;
    vert_out.tangent = a_Tangent;
    vert_out.biTangent = a_BiTangent;
    gl_Position = vec4(a_Pos, 1.0);
}