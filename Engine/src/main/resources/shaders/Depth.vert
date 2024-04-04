#version 330 core

layout (location = 0) in vec3 a_Pos;

uniform mat4 uModel;
uniform mat4 uCombined;

void main()
{
    gl_Position = uCombined * uModel * vec4(a_Pos, 1.0);
}