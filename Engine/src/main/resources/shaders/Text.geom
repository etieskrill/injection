#version 330 core

layout (points) in;
layout (triangle_strip, max_vertices = 4) out;

in vec2 tGlyphSize[1];
in vec2 tTexRatio[1];
flat in int tTexIndex[1];

out vec4 tColour;
out vec2 tTexCoords;
flat out int oTexIndex;

uniform vec2 uGlyphTextureSize;

void main()
{
    vec2 position = gl_in[0].gl_Position.xy;
    vec2 glyphSize = tGlyphSize[0];
    oTexIndex = tTexIndex[0];

    vec2 glyphTexSize = tTexRatio[0];

    gl_Position = vec4(position, 0, 1);
    tTexCoords = vec2(0.0);
    EmitVertex();

    gl_Position = vec4(position.x + glyphSize.x, position.y, 0, 1);
    tTexCoords = vec2(glyphTexSize.x, 0.0);
    EmitVertex();

    gl_Position = vec4(position.x, position.y - glyphSize.y, 0, 1);
    tTexCoords = vec2(0.0, glyphTexSize.y);
    EmitVertex();

    gl_Position = vec4(position.x + glyphSize.x, position.y - glyphSize.y, 0, 1);
    tTexCoords = vec2(glyphTexSize);
    EmitVertex();
}
