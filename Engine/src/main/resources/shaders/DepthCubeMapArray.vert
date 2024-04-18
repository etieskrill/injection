#version 330 core

layout (location = 0) in vec3 a_Pos;

uniform mat4 uModel;

void main()
{
    gl_Position = uModel * vec4(a_Pos, 1.0);
}