#version 330 core

struct Material {
    samplerCube cubemap0;
};

out vec4 oColour;

in vec3 tTextureDir;

uniform Material material;

void main()
{
    oColour = texture(material.cubemap0, tTextureDir);
}
