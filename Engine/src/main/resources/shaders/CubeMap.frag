#version 330 core

out vec4 oColour;

in vec3 tTextureDir;

uniform samplerCube uCubeMap;

void main()
{
    oColour = texture(uCubeMap, tTextureDir);
}
