#version 330 core

out vec4 oColour;

uniform vec4 colour;

void main()
{
    oColour = colour;
}