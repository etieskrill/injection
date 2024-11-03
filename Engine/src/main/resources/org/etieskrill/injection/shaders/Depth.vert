#version 330 core

layout (location = 0) in vec3 a_Pos;

uniform mat4 model;
uniform mat4 combined;

void main()
{
    gl_Position = combined * model * vec4(a_Pos, 1.0);
}