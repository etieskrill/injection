#version 330 core

#pragma stage vert
layout (location = 0) in vec3 a_Position;

out vec4 fragPos;

uniform mat4 mesh;
uniform mat4 model;
uniform mat4 combined;

void main()
{
    fragPos = combined * model * mesh * a_Position;
}

#pragma stage frag
out vec4 fragColour;

void main()
{
    fragColour = vec4(255, 0, 255);
}
