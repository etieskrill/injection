#version 330 core

in vec3 tNormal;
in vec2 tTextureCoords;
in vec3 tFragPos;

out vec4 oColour;

uniform float uTime;

void main()
{
    oColour = vec4(tTextureCoords.x, 0.25 * sin(uTime) + 0.25, tTextureCoords.y, 1.0);
}
