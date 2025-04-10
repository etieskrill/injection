#version 330 core

in vec3 a_Position;

uniform mat4 model;
uniform mat4 combined;

void main()
{
    gl_Position = combined * model * vec4(a_Position, 1.0);
}