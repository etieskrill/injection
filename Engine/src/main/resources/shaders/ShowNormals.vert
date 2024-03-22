#version 330 core

layout (location = 0) in vec3 a_Pos;
layout (location = 1) in vec3 a_Normal;

out vec3 t_Normal;

void main()
{
    t_Normal = a_Normal;
    gl_Position = vec4(a_Pos, 1.0);
}