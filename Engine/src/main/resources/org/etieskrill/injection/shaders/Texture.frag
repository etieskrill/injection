#version 330 core

out vec4 oColour;

in vec2 tTextureCoords;

struct Material {
    sampler2D diffuse0;
    int numTextures;
};

uniform Material material;

uniform vec4 colour;

void main()
{
    vec4 texel = material.numTextures > 0 ? texture(material.diffuse0, tTextureCoords) : vec4(1.0);
    if (texel.a < 0.1) discard;
    if (texel.r == 0 && texel.g == 0 && texel.b == 0) texel = colour;
    else texel *= colour;
    oColour = texel;
}
