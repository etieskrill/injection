#version 330 core

out vec4 oColour;

in vec2 tTextureCoords;

struct Material {
    sampler2D diffuse0;
};

uniform Material material;

void main()
{
    vec4 texel = texture(material.diffuse0, tTextureCoords).aaar;
    if (texel.a < 0.1) discard;
    oColour = texel;
}