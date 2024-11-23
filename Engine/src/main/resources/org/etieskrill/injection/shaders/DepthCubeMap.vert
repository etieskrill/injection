#version 330 core

layout (location = 0) in vec3 a_Pos;

uniform mat4 mesh;
uniform mat4 model;

void main()
{
    gl_Position = model * mesh * vec4(a_Pos, 1.0);
}