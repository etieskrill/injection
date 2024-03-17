#version 330 core

out vec4 oColour;

in vec2 tTextureCoords;

struct Material {
    sampler2D diffuse0;
    sampler2DArray array0;
};

uniform Material material;
uniform vec2 uGlyphSize;
uniform int uGlyphIndex;

void main()
{
    vec4 texel = texture(material.array0, vec3(tTextureCoords * uGlyphSize, uGlyphIndex));
    if (texel.a < 0.1) discard;
    oColour = texel;
}
