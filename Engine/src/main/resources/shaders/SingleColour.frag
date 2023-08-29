#version 330 core

out vec4 oColour;

uniform vec4 uColour;

void main()
{
    oColour = uColour;
}