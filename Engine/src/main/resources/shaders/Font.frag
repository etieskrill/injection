#version 330 core

out vec4 oColour;

in vec2 tTextureCoords;

struct Material {
    sampler2D diffuse0;
};

uniform Material material;

void main()
{
    vec4 texel = texture(material.diffuse0, tTextureCoords);
//    if (tTextureCoords.x <= 0.05 || tTextureCoords.x >= 0.95 || tTextureCoords.y <= 0.05 || tTextureCoords.y >= 0.95) {
//        texel = vec4(1.0, 0.0, 0.0, 1.0);
//    }
    if (texel.a < 0.1) discard;
    oColour = texel;
}
