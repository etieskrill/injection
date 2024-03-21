#version 330 core

out vec4 oColour;

in vec2 tTexCoords;
flat in int oTexIndex;

uniform sampler2DArray glyphs;

void main()
{
    vec4 texel = texture(glyphs, vec3(tTexCoords, oTexIndex));
    if (texel.a == 0.0) discard;
    oColour = texel;
}
